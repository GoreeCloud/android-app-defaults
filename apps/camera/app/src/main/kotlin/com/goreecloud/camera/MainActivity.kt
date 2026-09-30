// File internal version: 0.5.0
package com.goreecloud.camera

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.TextureView
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.goreecloud.camera.camera.CameraCapabilityRegistry
import com.goreecloud.camera.camera.CameraSessionController
import com.goreecloud.camera.camera.CameraSessionState
import com.goreecloud.camera.guidance.CameraGuidancePolicy
import com.goreecloud.camera.guidance.CameraGuidanceStore
import com.goreecloud.camera.settings.CameraSelfTimerPolicy
import com.goreecloud.camera.settings.CameraSettingsStore
import com.goreecloud.camera.settings.CameraVolumeShutterPolicy
import com.goreecloud.camera.ui.CompositionGridView

class MainActivity : Activity() {
    private lateinit var previewView: TextureView
    private lateinit var stateLabel: TextView
    private lateinit var capabilityLabel: TextView
    private lateinit var photoStatusLabel: TextView
    private lateinit var videoStatusLabel: TextView
    private lateinit var permissionButton: Button
    private lateinit var shutterButton: Button
    private lateinit var videoButton: Button
    private lateinit var settingsButton: Button
    private lateinit var guidanceButton: Button
    private lateinit var contextualHintLabel: TextView
    private lateinit var compositionGridView: CompositionGridView
    private lateinit var sessionController: CameraSessionController
    private lateinit var guidanceStore: CameraGuidanceStore
    private lateinit var settingsStore: CameraSettingsStore
    private val selfTimerHandler = Handler(Looper.getMainLooper())

    private var guidanceDialog: Dialog? = null
    private var settingsDialog: Dialog? = null
    private var currentSessionState = CameraSessionState.IDLE
    private var videoCapabilityAvailable = false
    private var pendingSelfTimer: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        guidanceStore = CameraGuidanceStore(this)
        settingsStore = CameraSettingsStore(this)
        buildInterface()

        sessionController = CameraSessionController(
            context = this,
            textureView = previewView,
            onStateChanged = { state, detail ->
                currentSessionState = state
                if (state != CameraSessionState.PREVIEWING && pendingSelfTimer != null) {
                    cancelPendingSelfTimer(showStatus = true)
                }
                val stateText = getString(R.string.session_status, state.name.lowercase())
                stateLabel.text = if (detail.isNullOrBlank()) stateText else "$stateText\n$detail"

                when (state) {
                    CameraSessionState.STARTING_VIDEO -> {
                        videoStatusLabel.text = getString(R.string.video_starting)
                    }
                    CameraSessionState.RECORDING -> {
                        videoStatusLabel.text = getString(R.string.video_recording)
                    }
                    CameraSessionState.STOPPING_VIDEO -> {
                        videoStatusLabel.text = getString(R.string.video_stopping)
                    }
                    else -> Unit
                }
                updateCaptureControls()
            },
            onPhotoCaptureFinished = { outcome ->
                photoStatusLabel.text = if (outcome.isSuccess) {
                    getString(R.string.photo_saved, outcome.displayName.orEmpty())
                } else {
                    getString(R.string.photo_failed, outcome.errorMessage.orEmpty())
                }
            },
            onVideoCapabilityChanged = { available ->
                videoCapabilityAvailable = available
                videoStatusLabel.text = when {
                    !available -> getString(R.string.video_status_unavailable)
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ->
                        getString(R.string.video_status_ready)
                    else -> getString(R.string.video_status_needs_microphone)
                }
                updateCaptureControls()
                refreshContextualHint()
            },
            onVideoRecordingFinished = { outcome ->
                videoStatusLabel.text = if (outcome.isSuccess) {
                    getString(R.string.video_saved, outcome.displayName.orEmpty())
                } else {
                    getString(R.string.video_failed, outcome.errorMessage.orEmpty())
                }
                updateCaptureControls()
            },
        )

        previewView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                maybeStartPreview()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                sessionController.stop()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }

        permissionButton.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }

        shutterButton.setOnClickListener {
            requestPhotoCaptureFromUserAction()
        }

        videoButton.setOnClickListener {
            when (currentSessionState) {
                CameraSessionState.RECORDING -> {
                    videoStatusLabel.text = getString(R.string.video_stopping)
                    sessionController.stopVideoRecording()
                }
                CameraSessionState.PREVIEWING -> beginVideoRecordingFromUserAction()
                else -> Unit
            }
        }

        settingsButton.setOnClickListener {
            if (guidanceStore.isFirstUseComplete()) {
                cancelPendingSelfTimer(showStatus = true)
                showSettingsMenu()
            }
        }

        guidanceButton.setOnClickListener {
            if (guidanceStore.isFirstUseComplete()) {
                cancelPendingSelfTimer(showStatus = true)
                showGuidanceMenu()
            }
        }

        refreshCompositionGrid()
        refreshCapabilities()
        renderPermissionState()
        updateCaptureControls()
        refreshContextualHint()

        if (!guidanceStore.isFirstUseComplete()) {
            showStartupGuide(replay = false)
        }
    }

    override fun onResume() {
        super.onResume()
        maybeStartPreview()
    }

    override fun onPause() {
        cancelPendingSelfTimer(showStatus = false)
        sessionController.stop()
        super.onPause()
    }

    override fun onDestroy() {
        cancelPendingSelfTimer(showStatus = false)
        guidanceDialog?.dismiss()
        settingsDialog?.dismiss()
        sessionController.shutdown()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey =
            keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val modalSurfaceVisible =
            guidanceDialog?.isShowing == true || settingsDialog?.isShowing == true
        if (
            CameraVolumeShutterPolicy.shouldCapture(
                enabled = settingsStore.isVolumeShutterEnabled(),
                isVolumeKey = isVolumeKey,
                repeatCount = event.repeatCount,
                sessionState = currentSessionState,
                modalSurfaceVisible = modalSurfaceVisible,
            )
        ) {
            requestPhotoCaptureFromUserAction()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun requestPhotoCaptureFromUserAction() {
        if (currentSessionState != CameraSessionState.PREVIEWING || pendingSelfTimer != null) return

        val timerSeconds = settingsStore.selfTimerSeconds()
        if (timerSeconds == CameraSelfTimerPolicy.OFF_SECONDS) {
            capturePhotoFromUserAction()
            return
        }

        photoStatusLabel.text = getString(R.string.photo_self_timer_started, timerSeconds)
        val action = Runnable {
            pendingSelfTimer = null
            val modalSurfaceVisible =
                guidanceDialog?.isShowing == true || settingsDialog?.isShowing == true
            if (currentSessionState == CameraSessionState.PREVIEWING && !modalSurfaceVisible) {
                capturePhotoFromUserAction()
            } else {
                photoStatusLabel.text = getString(R.string.photo_self_timer_cancelled)
            }
            updateCaptureControls()
        }
        pendingSelfTimer = action
        selfTimerHandler.postDelayed(action, timerSeconds * 1_000L)
        updateCaptureControls()
    }

    private fun cancelPendingSelfTimer(showStatus: Boolean) {
        val action = pendingSelfTimer ?: return
        selfTimerHandler.removeCallbacks(action)
        pendingSelfTimer = null
        if (showStatus && ::photoStatusLabel.isInitialized) {
            photoStatusLabel.text = getString(R.string.photo_self_timer_cancelled)
        }
        if (::shutterButton.isInitialized && ::videoButton.isInitialized) {
            updateCaptureControls()
        }
    }

    private fun capturePhotoFromUserAction() {
        if (currentSessionState != CameraSessionState.PREVIEWING) return
        photoStatusLabel.text = getString(R.string.photo_capture_in_progress)
        sessionController.capturePhoto()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                renderPermissionState()
                refreshCapabilities()
                maybeStartPreview()
            }
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                val granted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                videoStatusLabel.text = if (granted) {
                    getString(R.string.video_status_permission_granted)
                } else {
                    getString(R.string.video_status_permission_denied)
                }
                updateCaptureControls()
                refreshContextualHint()
            }
        }
    }

    private fun buildInterface() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        previewView = TextureView(this).apply {
            contentDescription = getString(R.string.preview_content_description)
        }
        root.addView(previewView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        compositionGridView = CompositionGridView(this).apply {
            visibility = View.GONE
        }
        root.addView(
            compositionGridView,
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT),
        )

        val topPanelPaddingHorizontal = dp(16)
        val topPanelPaddingTop = dp(12)
        val topPanelPaddingBottom = dp(12)
        val topPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                topPanelPaddingHorizontal,
                topPanelPaddingTop,
                topPanelPaddingHorizontal,
                topPanelPaddingBottom,
            )
            setBackgroundColor(Color.argb(168, 0, 0, 0))
        }

        val title = TextView(this).apply {
            text = getString(R.string.engineering_shell)
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        stateLabel = TextView(this).apply {
            text = getString(R.string.session_status, CameraSessionState.IDLE.name.lowercase())
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        }
        capabilityLabel = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        }
        contextualHintLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
            visibility = View.GONE
        }
        settingsButton = Button(this).apply {
            text = getString(R.string.camera_settings)
            contentDescription = getString(R.string.camera_settings_content_description)
            setAllCaps(false)
        }
        guidanceButton = Button(this).apply {
            text = getString(R.string.help_and_guidance)
            contentDescription = getString(R.string.help_and_guidance_content_description)
            setAllCaps(false)
        }
        val topActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        topActions.addView(settingsButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        topActions.addView(guidanceButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        topPanel.addView(title, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        topPanel.addView(stateLabel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        topPanel.addView(capabilityLabel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        topPanel.addView(contextualHintLabel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        topPanel.addView(topActions, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        root.addView(
            topPanel,
            FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP),
        )

        permissionButton = Button(this).apply {
            text = getString(R.string.grant_camera_permission)
            visibility = View.GONE
        }
        root.addView(
            permissionButton,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER),
        )

        val capturePaddingHorizontal = dp(16)
        val capturePaddingTop = dp(8)
        val capturePaddingBottom = dp(16)
        val capturePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                capturePaddingHorizontal,
                capturePaddingTop,
                capturePaddingHorizontal,
                capturePaddingBottom,
            )
            setBackgroundColor(Color.argb(168, 0, 0, 0))
        }

        photoStatusLabel = TextView(this).apply {
            text = getString(R.string.photo_status_ready)
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
        }
        shutterButton = Button(this).apply {
            text = getString(R.string.capture_photo)
            contentDescription = getString(R.string.capture_photo_content_description)
            isEnabled = false
        }
        videoStatusLabel = TextView(this).apply {
            text = getString(R.string.video_status_checking)
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        videoButton = Button(this).apply {
            text = getString(R.string.record_video)
            contentDescription = getString(R.string.record_video_content_description)
            isEnabled = false
        }

        capturePanel.addView(photoStatusLabel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        capturePanel.addView(shutterButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        capturePanel.addView(videoStatusLabel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        capturePanel.addView(videoButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        root.addView(
            capturePanel,
            FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM),
        )

        root.setOnApplyWindowInsetsListener { _, insets ->
            val systemInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars())
            } else {
                null
            }
            val topSystemInset = if (systemInsets != null) {
                systemInsets.top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }
            val bottomSystemInset = if (systemInsets != null) {
                systemInsets.bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            topPanel.setPadding(
                topPanelPaddingHorizontal,
                topPanelPaddingTop + topSystemInset,
                topPanelPaddingHorizontal,
                topPanelPaddingBottom,
            )
            capturePanel.setPadding(
                capturePaddingHorizontal,
                capturePaddingTop,
                capturePaddingHorizontal,
                capturePaddingBottom + bottomSystemInset,
            )
            insets
        }
        root.requestApplyInsets()

        setContentView(root)
    }

    private fun showStartupGuide(replay: Boolean) {
        guidanceDialog?.takeIf { it.isShowing }?.dismiss()
        if (replay) {
            guidanceStore.restartGuide()
        }

        val dialog = Dialog(this)
        guidanceDialog = dialog
        dialog.setCancelable(replay)
        dialog.setCanceledOnTouchOutside(false)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(20))
            setBackgroundColor(Color.rgb(24, 24, 24))
        }
        val title = TextView(this).apply {
            text = getString(R.string.startup_guide_title)
            setTextColor(Color.WHITE)
            textSize = 22f
        }
        val progress = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        }
        val stepTitle = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, dp(14), 0, 0)
        }
        val body = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 15f
            setPadding(0, dp(10), 0, dp(16))
        }
        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val backButton = Button(this).apply {
            text = getString(R.string.guide_back)
            setAllCaps(false)
        }
        val nextButton = Button(this).apply {
            setAllCaps(false)
        }
        val closeButton = Button(this).apply {
            text = getString(R.string.guide_close)
            setAllCaps(false)
            visibility = if (replay) View.VISIBLE else View.GONE
        }

        navigation.addView(backButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        navigation.addView(nextButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        navigation.addView(closeButton, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        panel.addView(title, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(progress, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(stepTitle, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(body, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(navigation, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        fun renderStep() {
            val step = guidanceStore.currentStep()
            progress.text = getString(
                R.string.startup_step_progress,
                step + 1,
                CameraGuidancePolicy.STEP_COUNT,
            )
            when (step) {
                0 -> {
                    stepTitle.text = getString(R.string.startup_step_local_title)
                    body.text = getString(R.string.startup_step_local_body)
                }
                1 -> {
                    stepTitle.text = getString(R.string.startup_step_permissions_title)
                    body.text = getString(R.string.startup_step_permissions_body)
                }
                else -> {
                    stepTitle.text = getString(R.string.startup_step_boundaries_title)
                    body.text = getString(R.string.startup_step_boundaries_body)
                }
            }
            backButton.isEnabled = step > 0
            nextButton.text = if (CameraGuidancePolicy.nextStep(step) == null) {
                getString(R.string.guide_finish)
            } else {
                getString(R.string.guide_next)
            }
        }

        backButton.setOnClickListener {
            guidanceStore.setCurrentStep(
                CameraGuidancePolicy.previousStep(guidanceStore.currentStep()),
            )
            renderStep()
        }
        nextButton.setOnClickListener {
            val next = CameraGuidancePolicy.nextStep(guidanceStore.currentStep())
            if (next == null) {
                if (!guidanceStore.isFirstUseComplete()) {
                    guidanceStore.completeFirstUse()
                } else {
                    guidanceStore.restartGuide()
                }
                dialog.dismiss()
                renderPermissionState()
                refreshContextualHint()
                maybeStartPreview()
            } else {
                guidanceStore.setCurrentStep(next)
                renderStep()
            }
        }
        closeButton.setOnClickListener {
            guidanceStore.restartGuide()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (guidanceDialog === dialog) {
                guidanceDialog = null
            }
        }
        dialog.setContentView(panel)
        renderStep()
        dialog.show()
        dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun showSettingsMenu() {
        settingsDialog?.takeIf { it.isShowing }?.dismiss()

        val dialog = Dialog(this)
        settingsDialog = dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(20))
            setBackgroundColor(Color.rgb(24, 24, 24))
        }
        val title = TextView(this).apply {
            text = getString(R.string.camera_settings_title)
            setTextColor(Color.WHITE)
            textSize = 22f
        }
        val body = TextView(this).apply {
            text = getString(R.string.camera_settings_body)
            setTextColor(Color.LTGRAY)
            textSize = 15f
            setPadding(0, dp(10), 0, dp(16))
        }
        val gridButton = Button(this).apply {
            setAllCaps(false)
        }
        val selfTimerButton = Button(this).apply {
            setAllCaps(false)
        }
        val volumeShutterButton = Button(this).apply {
            setAllCaps(false)
        }
        val closeButton = Button(this).apply {
            text = getString(R.string.guide_close)
            setAllCaps(false)
        }

        fun renderGridButton() {
            gridButton.text = if (settingsStore.isCompositionGridEnabled()) {
                getString(R.string.composition_grid_on)
            } else {
                getString(R.string.composition_grid_off)
            }
        }

        fun renderSelfTimerButton() {
            selfTimerButton.text = when (settingsStore.selfTimerSeconds()) {
                CameraSelfTimerPolicy.THREE_SECONDS -> getString(R.string.self_timer_3_seconds)
                CameraSelfTimerPolicy.TEN_SECONDS -> getString(R.string.self_timer_10_seconds)
                else -> getString(R.string.self_timer_off)
            }
        }

        fun renderVolumeShutterButton() {
            volumeShutterButton.text = if (settingsStore.isVolumeShutterEnabled()) {
                getString(R.string.volume_shutter_on)
            } else {
                getString(R.string.volume_shutter_off)
            }
        }

        gridButton.setOnClickListener {
            settingsStore.setCompositionGridEnabled(
                !settingsStore.isCompositionGridEnabled(),
            )
            renderGridButton()
            refreshCompositionGrid()
        }
        selfTimerButton.setOnClickListener {
            settingsStore.setSelfTimerSeconds(
                CameraSelfTimerPolicy.nextSeconds(settingsStore.selfTimerSeconds()),
            )
            renderSelfTimerButton()
        }
        volumeShutterButton.setOnClickListener {
            settingsStore.setVolumeShutterEnabled(
                !settingsStore.isVolumeShutterEnabled(),
            )
            renderVolumeShutterButton()
        }
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        panel.addView(title, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(body, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(gridButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(selfTimerButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(volumeShutterButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(closeButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        dialog.setOnDismissListener {
            if (settingsDialog === dialog) {
                settingsDialog = null
            }
        }
        dialog.setContentView(panel)
        renderGridButton()
        renderSelfTimerButton()
        renderVolumeShutterButton()
        dialog.show()
        dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun showGuidanceMenu() {
        guidanceDialog?.takeIf { it.isShowing }?.dismiss()

        val dialog = Dialog(this)
        guidanceDialog = dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(20))
            setBackgroundColor(Color.rgb(24, 24, 24))
        }
        val title = TextView(this).apply {
            text = getString(R.string.guidance_menu_title)
            setTextColor(Color.WHITE)
            textSize = 22f
        }
        val body = TextView(this).apply {
            text = getString(R.string.guidance_menu_body)
            setTextColor(Color.LTGRAY)
            textSize = 15f
            setPadding(0, dp(10), 0, dp(16))
        }
        val replayButton = Button(this).apply {
            text = getString(R.string.replay_startup_guide)
            setAllCaps(false)
        }
        val hintButton = Button(this).apply {
            setAllCaps(false)
        }
        val closeButton = Button(this).apply {
            text = getString(R.string.guide_close)
            setAllCaps(false)
        }

        fun renderHintButton() {
            hintButton.text = if (guidanceStore.areContextualHintsEnabled()) {
                getString(R.string.turn_contextual_hints_off)
            } else {
                getString(R.string.turn_contextual_hints_on)
            }
        }

        replayButton.setOnClickListener {
            dialog.dismiss()
            showStartupGuide(replay = true)
        }
        hintButton.setOnClickListener {
            guidanceStore.setContextualHintsEnabled(
                !guidanceStore.areContextualHintsEnabled(),
            )
            renderHintButton()
            refreshContextualHint()
        }
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        panel.addView(title, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(body, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(replayButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(hintButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        panel.addView(closeButton, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        dialog.setOnDismissListener {
            if (guidanceDialog === dialog) {
                guidanceDialog = null
            }
        }
        dialog.setContentView(panel)
        renderHintButton()
        dialog.show()
        dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun refreshCompositionGrid() {
        if (!::compositionGridView.isInitialized || !::settingsStore.isInitialized) return
        compositionGridView.visibility =
            if (settingsStore.isCompositionGridEnabled()) View.VISIBLE else View.GONE
    }

    private fun refreshContextualHint() {
        if (!::contextualHintLabel.isInitialized || !::guidanceStore.isInitialized) return

        val visible = guidanceStore.isFirstUseComplete() &&
            guidanceStore.areContextualHintsEnabled()
        contextualHintLabel.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return

        contextualHintLabel.text = when {
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ->
                getString(R.string.contextual_hint_camera_permission)
            videoCapabilityAvailable &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ->
                getString(R.string.contextual_hint_microphone)
            else -> getString(R.string.contextual_hint_local_media)
        }
    }

    private fun beginVideoRecordingFromUserAction() {
        if (!videoCapabilityAvailable) {
            videoStatusLabel.text = getString(R.string.video_status_unavailable)
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            videoStatusLabel.text = getString(R.string.video_status_needs_microphone)
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION,
            )
            return
        }

        videoStatusLabel.text = getString(R.string.video_starting)
        sessionController.startVideoRecording()
    }

    private fun maybeStartPreview() {
        if (!::sessionController.isInitialized || !::guidanceStore.isInitialized) return
        if (!guidanceStore.isFirstUseComplete()) {
            sessionController.stop()
            return
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            permissionButton.visibility = View.GONE
            sessionController.start()
        } else {
            sessionController.stop()
            renderPermissionState()
        }
    }

    private fun renderPermissionState() {
        val granted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        permissionButton.visibility = if (granted) View.GONE else View.VISIBLE
        if (!granted) {
            stateLabel.text = getString(R.string.permission_required)
        }
        updateCaptureControls()
        refreshContextualHint()
    }

    private fun updateCaptureControls() {
        val cameraGranted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val selfTimerIdle = pendingSelfTimer == null
        shutterButton.isEnabled =
            cameraGranted && currentSessionState == CameraSessionState.PREVIEWING && selfTimerIdle

        when (currentSessionState) {
            CameraSessionState.RECORDING -> {
                videoButton.isEnabled = true
                videoButton.text = getString(R.string.stop_video)
                videoButton.contentDescription = getString(R.string.stop_video_content_description)
            }
            CameraSessionState.PREVIEWING -> {
                videoButton.isEnabled = cameraGranted && videoCapabilityAvailable && selfTimerIdle
                videoButton.text = getString(R.string.record_video)
                videoButton.contentDescription = getString(R.string.record_video_content_description)
            }
            else -> {
                videoButton.isEnabled = false
                videoButton.text = getString(R.string.record_video)
                videoButton.contentDescription = getString(R.string.record_video_content_description)
            }
        }
    }

    private fun refreshCapabilities() {
        val manager = getSystemService(CameraManager::class.java)
        val profiles = CameraCapabilityRegistry(manager).profiles()
        capabilityLabel.text = if (profiles.isEmpty()) {
            getString(R.string.camera_unavailable)
        } else {
            getString(R.string.capability_summary, profiles.size)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REQUEST_CAMERA_PERMISSION = 1001
        const val REQUEST_RECORD_AUDIO_PERMISSION = 1002
    }
}
