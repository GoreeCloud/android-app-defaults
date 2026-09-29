package com.goreecloud.camera.settings

import com.goreecloud.camera.camera.CameraSessionState

object CameraVolumeShutterPolicy {
    fun shouldCapture(
        enabled: Boolean,
        isVolumeKey: Boolean,
        repeatCount: Int,
        sessionState: CameraSessionState,
        modalSurfaceVisible: Boolean,
    ): Boolean =
        enabled &&
            isVolumeKey &&
            repeatCount == 0 &&
            sessionState == CameraSessionState.PREVIEWING &&
            !modalSurfaceVisible
}
