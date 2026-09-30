// File internal version: 0.4.0
package com.goreecloud.camera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.goreecloud.camera.storage.PendingPhoto
import com.goreecloud.camera.storage.PendingVideo
import com.goreecloud.camera.storage.PhotoMediaStoreCommitter
import com.goreecloud.camera.storage.VideoMediaStoreCommitter
import java.util.concurrent.Executor

enum class CameraSessionState {
    IDLE,
    WAITING_FOR_SURFACE,
    OPENING,
    PREVIEWING,
    CAPTURING,
    STARTING_VIDEO,
    RECORDING,
    STOPPING_VIDEO,
    ERROR,
}

data class PhotoCaptureOutcome(
    val displayName: String? = null,
    val uri: Uri? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean
        get() = uri != null && errorMessage == null
}

data class VideoRecordingOutcome(
    val displayName: String? = null,
    val uri: Uri? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean
        get() = uri != null && errorMessage == null
}

class CameraSessionController(
    context: Context,
    private val textureView: TextureView,
    private val onStateChanged: (CameraSessionState, String?) -> Unit,
    private val onPhotoCaptureFinished: (PhotoCaptureOutcome) -> Unit = {},
    private val onVideoCapabilityChanged: (Boolean) -> Unit = {},
    private val onVideoRecordingFinished: (VideoRecordingOutcome) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)
    private val capabilityRegistry = CameraCapabilityRegistry(cameraManager)
    private val photoCommitter = PhotoMediaStoreCommitter(appContext.contentResolver)
    private val videoCommitter = VideoMediaStoreCommitter(appContext.contentResolver)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cameraThread = HandlerThread("GoreeCloudCameraSession").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private val cameraExecutor = Executor { command -> cameraHandler.post(command) }

    @Volatile
    private var desiredActive = false

    @Volatile
    private var opening = false

    @Volatile
    private var sessionState = CameraSessionState.IDLE

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private var pendingPhoto: PendingPhoto? = null
    private var captureInFlight = false
    private var videoSize: Size? = null
    private var mediaRecorder: MediaRecorder? = null
    private var pendingVideo: PendingVideo? = null
    private var videoInFlight = false

    fun start() {
        desiredActive = true
        openIfReady()
    }

    @Synchronized
    fun stop() {
        desiredActive = false
        closeSessionResources()
        transition(CameraSessionState.IDLE, null)
    }

    fun shutdown() {
        stop()
        cameraThread.quitSafely()
        if (Thread.currentThread() !== cameraThread) {
            runCatching { cameraThread.join(1_000) }
        }
    }

    @Synchronized
    fun capturePhoto() {
        if (!desiredActive || sessionState != CameraSessionState.PREVIEWING) {
            notifyPhotoOutcome(
                PhotoCaptureOutcome(errorMessage = "Camera preview is not ready for capture"),
            )
            return
        }
        if (captureInFlight || videoInFlight) {
            notifyPhotoOutcome(
                PhotoCaptureOutcome(errorMessage = "Another capture operation is already in progress"),
            )
            return
        }

        captureInFlight = true
        transition(CameraSessionState.CAPTURING, null)
        cameraHandler.post(::captureStillImage)
    }

    @Synchronized
    fun startVideoRecording() {
        if (!desiredActive || sessionState != CameraSessionState.PREVIEWING) {
            notifyVideoOutcome(
                VideoRecordingOutcome(errorMessage = "Camera preview is not ready for video recording"),
            )
            return
        }
        if (captureInFlight || videoInFlight) {
            notifyVideoOutcome(
                VideoRecordingOutcome(errorMessage = "Another capture operation is already in progress"),
            )
            return
        }
        if (!hasMicrophone()) {
            notifyVideoOutcome(
                VideoRecordingOutcome(errorMessage = "This device does not report microphone capability"),
            )
            return
        }
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifyVideoOutcome(
                VideoRecordingOutcome(errorMessage = "Microphone permission is required for video audio"),
            )
            return
        }

        val selectedVideoSize = videoSize
        if (selectedVideoSize == null) {
            notifyVideoOutcome(
                VideoRecordingOutcome(errorMessage = "This camera has no compatible bounded video output"),
            )
            return
        }

        videoInFlight = true
        transition(
            CameraSessionState.STARTING_VIDEO,
            "video=${selectedVideoSize.width}x${selectedVideoSize.height}, audio=microphone",
        )
        cameraHandler.post { beginVideoRecording(selectedVideoSize) }
    }

    @Synchronized
    fun stopVideoRecording() {
        if (!videoInFlight || sessionState != CameraSessionState.RECORDING) {
            notifyVideoOutcome(
                VideoRecordingOutcome(errorMessage = "No video recording is active"),
            )
            return
        }

        transition(CameraSessionState.STOPPING_VIDEO, null)
        cameraHandler.post(::finishVideoRecording)
    }

    @Synchronized
    private fun openIfReady() {
        if (!desiredActive) return
        if (cameraDevice != null || opening) return

        if (appContext.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            transition(CameraSessionState.ERROR, "Camera permission is not granted")
            return
        }

        if (!textureView.isAvailable) {
            transition(CameraSessionState.WAITING_FOR_SURFACE, null)
            return
        }

        val profiles = capabilityRegistry.profiles()
        val selected = CameraSelector.selectDefaultCamera(profiles.map { it.descriptor })
        if (selected == null) {
            transition(CameraSessionState.ERROR, "No camera is available")
            return
        }

        val profile = profiles.firstOrNull { it.descriptor.id == selected.id }
        val previewSize = profile?.let {
            capabilityRegistry.selectPreviewSize(it, textureView.width, textureView.height)
        }
        val jpegSize = profile?.let(capabilityRegistry::selectJpegSize)
        val selectedVideoSize = profile?.let(capabilityRegistry::selectVideoSize)
        val surfaceTexture = textureView.surfaceTexture
        if (profile == null || previewSize == null || jpegSize == null || surfaceTexture == null) {
            transition(CameraSessionState.ERROR, "No compatible preview and JPEG configuration is available")
            return
        }

        videoSize = selectedVideoSize
        notifyVideoCapability(selectedVideoSize != null && hasMicrophone())

        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        previewSurface?.release()
        previewSurface = Surface(surfaceTexture)

        imageReader?.close()
        imageReader = ImageReader.newInstance(
            jpegSize.width,
            jpegSize.height,
            ImageFormat.JPEG,
            MAX_IMAGES,
        ).apply {
            setOnImageAvailableListener({ reader -> handlePhotoImageAvailable(reader) }, cameraHandler)
        }

        opening = true
        val videoDetail = selectedVideoSize?.let {
            ", video-candidate=${it.width}x${it.height}, mic=${if (hasMicrophone()) "available" else "unavailable"}"
        } ?: ", video-candidate=unavailable"
        transition(
            CameraSessionState.OPENING,
            "camera=${selected.id}, preview=${previewSize.width}x${previewSize.height}, jpeg=${jpegSize.width}x${jpegSize.height}$videoDetail",
        )
        openCamera(selected.id)
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String) {
        try {
            cameraManager.openCamera(
                cameraId,
                cameraExecutor,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        synchronized(this@CameraSessionController) {
                            opening = false
                            if (!desiredActive) {
                                camera.close()
                                return
                            }
                            cameraDevice = camera
                        }
                        createCaptureSession(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        synchronized(this@CameraSessionController) {
                            opening = false
                            if (cameraDevice === camera) cameraDevice = null
                            camera.close()
                        }
                        failPendingPhoto("Camera disconnected")
                        failPendingVideo("Camera disconnected", restorePreview = false)
                        transition(CameraSessionState.ERROR, "Camera disconnected")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        synchronized(this@CameraSessionController) {
                            opening = false
                            if (cameraDevice === camera) cameraDevice = null
                            camera.close()
                        }
                        failPendingPhoto("Camera error code $error")
                        failPendingVideo("Camera error code $error", restorePreview = false)
                        transition(CameraSessionState.ERROR, "Camera error code $error")
                    }
                },
            )
        } catch (_: SecurityException) {
            opening = false
            transition(CameraSessionState.ERROR, "Camera permission changed while opening")
        } catch (exception: Exception) {
            opening = false
            transition(CameraSessionState.ERROR, exception.message ?: "Unable to open camera")
        }
    }

    private fun createCaptureSession(camera: CameraDevice) {
        val preview = synchronized(this) { previewSurface }
        val photoSurface = synchronized(this) { imageReader?.surface }
        if (!desiredActive || preview == null || photoSurface == null) {
            camera.close()
            return
        }

        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(OutputConfiguration(preview), OutputConfiguration(photoSurface)),
            cameraExecutor,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (!desiredActive || videoInFlight) {
                        session.close()
                        return
                    }

                    val request = try {
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(preview)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        }.build()
                    } catch (exception: Exception) {
                        session.close()
                        transition(CameraSessionState.ERROR, exception.message ?: "Preview request failed")
                        return
                    }

                    synchronized(this@CameraSessionController) {
                        captureSession?.close()
                        captureSession = session
                    }

                    try {
                        session.setRepeatingRequest(request, null, cameraHandler)
                        transition(CameraSessionState.PREVIEWING, null)
                    } catch (exception: Exception) {
                        session.close()
                        synchronized(this@CameraSessionController) {
                            if (captureSession === session) captureSession = null
                        }
                        transition(CameraSessionState.ERROR, exception.message ?: "Preview could not start")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    session.close()
                    transition(CameraSessionState.ERROR, "Camera capture session configuration failed")
                }
            },
        )

        try {
            camera.createCaptureSession(sessionConfiguration)
        } catch (exception: Exception) {
            transition(CameraSessionState.ERROR, exception.message ?: "Capture session could not be created")
        }
    }

    private fun captureStillImage() {
        val camera: CameraDevice
        val session: CameraCaptureSession
        val photoSurface: Surface

        synchronized(this) {
            camera = cameraDevice ?: return finishPhotoFailure("Camera device is unavailable")
            session = captureSession ?: return finishPhotoFailure("Camera session is unavailable")
            photoSurface = imageReader?.surface ?: return finishPhotoFailure("JPEG output is unavailable")
        }

        val reserved = try {
            photoCommitter.reserve()
        } catch (exception: Exception) {
            finishPhotoFailure(exception.message ?: "Unable to reserve photo storage")
            return
        }

        synchronized(this) {
            if (!desiredActive || !captureInFlight) {
                photoCommitter.discard(reserved)
                return
            }
            pendingPhoto = reserved
        }

        val request = try {
            camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(photoSurface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            }.build()
        } catch (exception: Exception) {
            failPendingPhoto(exception.message ?: "Unable to create still-capture request")
            return
        }

        try {
            session.capture(
                request,
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure,
                    ) {
                        failPendingPhoto("Still capture failed with reason ${failure.reason}")
                    }
                },
                cameraHandler,
            )
        } catch (exception: Exception) {
            failPendingPhoto(exception.message ?: "Unable to submit still-capture request")
        }
    }

    private fun handlePhotoImageAvailable(reader: ImageReader) {
        val image = runCatching { reader.acquireNextImage() }.getOrNull() ?: return
        try {
            val plane = image.planes.firstOrNull()
            if (plane == null) {
                failPendingPhoto("JPEG image contained no readable plane")
                return
            }

            val buffer = plane.buffer
            val jpegBytes = ByteArray(buffer.remaining())
            buffer.get(jpegBytes)

            val reserved = synchronized(this) {
                val photo = pendingPhoto
                pendingPhoto = null
                photo
            } ?: return

            val uri = try {
                photoCommitter.commit(reserved, jpegBytes)
            } catch (exception: Exception) {
                finishPhotoFailure(exception.message ?: "Unable to finalize photo")
                return
            }

            val activeAfterCapture = synchronized(this) {
                captureInFlight = false
                desiredActive
            }
            transition(
                if (activeAfterCapture) CameraSessionState.PREVIEWING else CameraSessionState.IDLE,
                null,
            )
            notifyPhotoOutcome(
                PhotoCaptureOutcome(
                    displayName = reserved.displayName,
                    uri = uri,
                ),
            )
        } finally {
            image.close()
        }
    }

    private fun beginVideoRecording(selectedVideoSize: Size) {
        val camera = synchronized(this) { cameraDevice }
        val preview = synchronized(this) { previewSurface }
        if (camera == null || preview == null || !desiredActive) {
            failPendingVideo("Camera is unavailable for video recording")
            return
        }
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            failPendingVideo("Microphone permission changed before recording began")
            return
        }

        val reserved = try {
            videoCommitter.reserve()
        } catch (exception: Exception) {
            failPendingVideo(exception.message ?: "Unable to reserve video storage")
            return
        }

        val recorder = try {
            createPreparedMediaRecorder(reserved, selectedVideoSize)
        } catch (exception: Exception) {
            videoCommitter.discard(reserved)
            failPendingVideo(exception.message ?: "Unable to prepare video/audio recorder")
            return
        }

        synchronized(this) {
            if (!desiredActive || !videoInFlight) {
                runCatching { recorder.reset() }
                recorder.release()
                videoCommitter.discard(reserved)
                return
            }
            pendingVideo = reserved
            mediaRecorder = recorder
            captureSession?.close()
            captureSession = null
        }

        configureRecordingSession(camera, preview, recorder)
    }

    @Suppress("DEPRECATION")
    private fun createPreparedMediaRecorder(
        pending: PendingVideo,
        selectedVideoSize: Size,
    ): MediaRecorder = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(pending.fileDescriptor.fileDescriptor)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioChannels(AUDIO_CHANNELS)
        setAudioSamplingRate(AUDIO_SAMPLE_RATE)
        setAudioEncodingBitRate(AUDIO_BIT_RATE)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setVideoSize(selectedVideoSize.width, selectedVideoSize.height)
        setVideoFrameRate(VIDEO_FRAME_RATE)
        setVideoEncodingBitRate(
            if (selectedVideoSize.width.toLong() * selectedVideoSize.height.toLong() >= 1_500_000L) {
                VIDEO_BIT_RATE_HIGH
            } else {
                VIDEO_BIT_RATE_STANDARD
            },
        )
        prepare()
    }

    private fun configureRecordingSession(
        camera: CameraDevice,
        preview: Surface,
        recorder: MediaRecorder,
    ) {
        val recordSurface = try {
            recorder.surface
        } catch (exception: Exception) {
            failPendingVideo(exception.message ?: "Video encoder surface is unavailable")
            return
        }

        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(OutputConfiguration(preview), OutputConfiguration(recordSurface)),
            cameraExecutor,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (!desiredActive || !videoInFlight || mediaRecorder !== recorder) {
                        session.close()
                        return
                    }

                    val request = try {
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            addTarget(preview)
                            addTarget(recordSurface)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        }.build()
                    } catch (exception: Exception) {
                        session.close()
                        failPendingVideo(exception.message ?: "Unable to create video request")
                        return
                    }

                    synchronized(this@CameraSessionController) {
                        captureSession = session
                    }

                    try {
                        session.setRepeatingRequest(request, null, cameraHandler)
                        recorder.start()
                        transition(
                            CameraSessionState.RECORDING,
                            "MP4 H.264 + AAC · microphone active",
                        )
                    } catch (exception: Exception) {
                        session.close()
                        synchronized(this@CameraSessionController) {
                            if (captureSession === session) captureSession = null
                        }
                        failPendingVideo(exception.message ?: "Unable to start video/audio recording")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    session.close()
                    failPendingVideo("Camera video session configuration failed")
                }
            },
        )

        try {
            camera.createCaptureSession(sessionConfiguration)
        } catch (exception: Exception) {
            failPendingVideo(exception.message ?: "Video capture session could not be created")
        }
    }

    private fun finishVideoRecording() {
        val recorder: MediaRecorder
        val reserved: PendingVideo
        val session: CameraCaptureSession?

        synchronized(this) {
            recorder = mediaRecorder ?: return failPendingVideo("Video recorder is unavailable")
            reserved = pendingVideo ?: return failPendingVideo("Pending video destination is unavailable")
            session = captureSession
        }

        runCatching { session?.stopRepeating() }
        runCatching { session?.abortCaptures() }

        val stopFailure = runCatching { recorder.stop() }.exceptionOrNull()
        runCatching { recorder.reset() }
        recorder.release()

        synchronized(this) {
            if (mediaRecorder === recorder) mediaRecorder = null
            captureSession?.close()
            captureSession = null
            if (pendingVideo === reserved) pendingVideo = null
            videoInFlight = false
        }

        if (stopFailure != null) {
            videoCommitter.discard(reserved)
            notifyVideoOutcome(
                VideoRecordingOutcome(
                    errorMessage = stopFailure.message ?: "Video/audio recording did not finalize",
                ),
            )
            restorePreviewAfterVideo()
            return
        }

        val uri = try {
            videoCommitter.publish(reserved)
        } catch (exception: Exception) {
            notifyVideoOutcome(
                VideoRecordingOutcome(
                    errorMessage = exception.message ?: "Unable to publish video recording",
                ),
            )
            restorePreviewAfterVideo()
            return
        }

        notifyVideoOutcome(
            VideoRecordingOutcome(
                displayName = reserved.displayName,
                uri = uri,
            ),
        )
        restorePreviewAfterVideo()
    }

    private fun restorePreviewAfterVideo() {
        val camera = synchronized(this) { cameraDevice }
        if (desiredActive && camera != null) {
            createCaptureSession(camera)
        } else {
            transition(CameraSessionState.IDLE, null)
        }
    }

    private fun failPendingPhoto(message: String) {
        val reserved = synchronized(this) {
            val photo = pendingPhoto
            pendingPhoto = null
            photo
        }
        if (reserved != null) {
            photoCommitter.discard(reserved)
        }
        finishPhotoFailure(message)
    }

    private fun finishPhotoFailure(message: String) {
        synchronized(this) {
            captureInFlight = false
        }
        if (desiredActive && sessionState != CameraSessionState.ERROR && !videoInFlight) {
            transition(CameraSessionState.PREVIEWING, null)
        }
        notifyPhotoOutcome(PhotoCaptureOutcome(errorMessage = message))
    }

    private fun failPendingVideo(
        message: String,
        restorePreview: Boolean = true,
    ) {
        val reserved: PendingVideo?
        val recorder: MediaRecorder?
        synchronized(this) {
            reserved = pendingVideo
            pendingVideo = null
            recorder = mediaRecorder
            mediaRecorder = null
            videoInFlight = false
            captureSession?.close()
            captureSession = null
        }

        if (recorder != null) {
            runCatching { recorder.reset() }
            recorder.release()
        }
        if (reserved != null) {
            videoCommitter.discard(reserved)
        }
        notifyVideoOutcome(VideoRecordingOutcome(errorMessage = message))

        if (restorePreview) {
            restorePreviewAfterVideo()
        }
    }

    @Synchronized
    private fun closeSessionResources() {
        pendingPhoto?.let(photoCommitter::discard)
        pendingPhoto = null
        captureInFlight = false

        mediaRecorder?.let { recorder ->
            runCatching { recorder.reset() }
            recorder.release()
        }
        mediaRecorder = null
        pendingVideo?.let(videoCommitter::discard)
        pendingVideo = null
        videoInFlight = false
        videoSize = null
        notifyVideoCapability(false)

        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        previewSurface?.release()
        previewSurface = null
        imageReader?.close()
        imageReader = null
    }

    private fun hasMicrophone(): Boolean =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    private fun transition(state: CameraSessionState, detail: String?) {
        sessionState = state
        mainHandler.post { onStateChanged(state, detail) }
    }

    private fun notifyPhotoOutcome(outcome: PhotoCaptureOutcome) {
        mainHandler.post { onPhotoCaptureFinished(outcome) }
    }

    private fun notifyVideoCapability(available: Boolean) {
        mainHandler.post { onVideoCapabilityChanged(available) }
    }

    private fun notifyVideoOutcome(outcome: VideoRecordingOutcome) {
        mainHandler.post { onVideoRecordingFinished(outcome) }
    }

    private companion object {
        const val MAX_IMAGES = 2
        const val VIDEO_FRAME_RATE = 30
        const val VIDEO_BIT_RATE_STANDARD = 4_000_000
        const val VIDEO_BIT_RATE_HIGH = 8_000_000
        const val AUDIO_CHANNELS = 1
        const val AUDIO_SAMPLE_RATE = 48_000
        const val AUDIO_BIT_RATE = 128_000
    }
}
