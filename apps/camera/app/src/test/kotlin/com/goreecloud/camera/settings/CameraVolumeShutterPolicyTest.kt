package com.goreecloud.camera.settings

import com.goreecloud.camera.camera.CameraSessionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraVolumeShutterPolicyTest {
    @Test
    fun readyPreviewAllowsFirstVolumeKeyPress() {
        assertTrue(
            CameraVolumeShutterPolicy.shouldCapture(
                enabled = true,
                isVolumeKey = true,
                repeatCount = 0,
                sessionState = CameraSessionState.PREVIEWING,
                modalSurfaceVisible = false,
            ),
        )
    }

    @Test
    fun disabledRepeatedBusyAndModalStatesFailClosed() {
        assertFalse(
            CameraVolumeShutterPolicy.shouldCapture(
                enabled = false,
                isVolumeKey = true,
                repeatCount = 0,
                sessionState = CameraSessionState.PREVIEWING,
                modalSurfaceVisible = false,
            ),
        )
        assertFalse(
            CameraVolumeShutterPolicy.shouldCapture(
                enabled = true,
                isVolumeKey = true,
                repeatCount = 1,
                sessionState = CameraSessionState.PREVIEWING,
                modalSurfaceVisible = false,
            ),
        )
        assertFalse(
            CameraVolumeShutterPolicy.shouldCapture(
                enabled = true,
                isVolumeKey = true,
                repeatCount = 0,
                sessionState = CameraSessionState.CAPTURING,
                modalSurfaceVisible = false,
            ),
        )
        assertFalse(
            CameraVolumeShutterPolicy.shouldCapture(
                enabled = true,
                isVolumeKey = true,
                repeatCount = 0,
                sessionState = CameraSessionState.PREVIEWING,
                modalSurfaceVisible = true,
            ),
        )
    }
}
