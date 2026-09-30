// File internal version: 0.3.0
package com.goreecloud.camera.camera

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Size
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CameraProfile(
    val descriptor: CameraDescriptor,
    val hardwareLevel: Int,
    val supportsRaw: Boolean,
    val supportsLogicalMultiCamera: Boolean,
    val previewSizes: List<Size>,
    val jpegSizes: List<Size>,
    val videoSizes: List<Size>,
)

class CameraCapabilityRegistry(
    private val cameraManager: CameraManager,
) {
    fun profiles(): List<CameraProfile> = try {
        cameraManager.cameraIdList.mapNotNull(::readProfile)
    } catch (_: CameraAccessException) {
        emptyList()
    }

    fun selectPreviewSize(profile: CameraProfile, viewWidth: Int, viewHeight: Int): Size? {
        if (profile.previewSizes.isEmpty()) return null

        val targetLong = max(viewWidth, viewHeight).coerceAtLeast(1)
        val targetShort = min(viewWidth, viewHeight).coerceAtLeast(1)
        val targetRatio = targetLong.toDouble() / targetShort.toDouble()
        val targetArea = targetLong.toLong() * targetShort.toLong()

        val bounded = profile.previewSizes.filter { size ->
            val longSide = max(size.width, size.height)
            val shortSide = min(size.width, size.height)
            longSide <= 1920 && shortSide <= 1080
        }
        val candidates = bounded.ifEmpty { profile.previewSizes }

        return candidates.minWithOrNull(
            compareBy<Size>(
                { size ->
                    val longSide = max(size.width, size.height)
                    val shortSide = min(size.width, size.height).coerceAtLeast(1)
                    abs((longSide.toDouble() / shortSide.toDouble()) - targetRatio)
                },
                { size -> abs((size.width.toLong() * size.height.toLong()) - targetArea) },
            ),
        )
    }

    fun selectJpegSize(profile: CameraProfile): Size? {
        if (profile.jpegSizes.isEmpty()) return null

        val bounded = profile.jpegSizes.filter { size ->
            size.width.toLong() * size.height.toLong() <= MAX_INITIAL_JPEG_PIXELS
        }
        return (bounded.ifEmpty { profile.jpegSizes })
            .maxByOrNull { size -> size.width.toLong() * size.height.toLong() }
    }

    fun selectVideoSize(profile: CameraProfile): Size? {
        if (profile.videoSizes.isEmpty()) return null

        val bounded = profile.videoSizes.filter { size ->
            val longSide = max(size.width, size.height)
            val shortSide = min(size.width, size.height)
            longSide <= 1920 && shortSide <= 1080
        }
        return (bounded.ifEmpty { profile.videoSizes })
            .maxByOrNull { size -> size.width.toLong() * size.height.toLong() }
    }

    private fun readProfile(cameraId: String): CameraProfile? {
        val characteristics = try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (_: CameraAccessException) {
            return null
        }

        val lensFacing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_BACK -> LensFacing.BACK
            CameraCharacteristics.LENS_FACING_FRONT -> LensFacing.FRONT
            CameraCharacteristics.LENS_FACING_EXTERNAL -> LensFacing.EXTERNAL
            else -> LensFacing.UNKNOWN
        }

        val capabilities = characteristics
            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.toSet()
            .orEmpty()

        val streamConfiguration = characteristics
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        val previewSizes = streamConfiguration
            ?.getOutputSizes(SurfaceTexture::class.java)
            ?.toList()
            .orEmpty()

        val jpegSizes = streamConfiguration
            ?.getOutputSizes(ImageFormat.JPEG)
            ?.toList()
            .orEmpty()

        val videoSizes = streamConfiguration
            ?.getOutputSizes(MediaRecorder::class.java)
            ?.toList()
            .orEmpty()

        return CameraProfile(
            descriptor = CameraDescriptor(cameraId, lensFacing),
            hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: -1,
            supportsRaw = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW in capabilities,
            supportsLogicalMultiCamera = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA in capabilities,
            previewSizes = previewSizes,
            jpegSizes = jpegSizes,
            videoSizes = videoSizes,
        )
    }

    private companion object {
        const val MAX_INITIAL_JPEG_PIXELS = 12_000_000L
    }
}
