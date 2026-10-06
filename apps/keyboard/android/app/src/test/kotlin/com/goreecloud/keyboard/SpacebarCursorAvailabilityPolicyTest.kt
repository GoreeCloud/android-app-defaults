package com.goreecloud.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacebarCursorAvailabilityPolicyTest {
    @Test
    fun ordinaryEditorAllowsConfiguredCursorControl() {
        assertTrue(
            SpacebarCursorAvailabilityPolicy.isEnabled(
                settingEnabled = true,
                sensitiveInput = false,
                layer = KeyboardLayer.LETTERS,
                touchExplorationEnabled = false,
            ),
        )
    }

    @Test
    fun disabledSettingFailsClosed() {
        assertFalse(
            SpacebarCursorAvailabilityPolicy.isEnabled(
                settingEnabled = false,
                sensitiveInput = false,
                layer = KeyboardLayer.LETTERS,
                touchExplorationEnabled = false,
            ),
        )
    }

    @Test
    fun sensitiveEditorFailsClosed() {
        assertFalse(
            SpacebarCursorAvailabilityPolicy.isEnabled(
                settingEnabled = true,
                sensitiveInput = true,
                layer = KeyboardLayer.LETTERS,
                touchExplorationEnabled = false,
            ),
        )
    }

    @Test
    fun emojiLayerAndTouchExplorationFailClosed() {
        assertFalse(
            SpacebarCursorAvailabilityPolicy.isEnabled(
                settingEnabled = true,
                sensitiveInput = false,
                layer = KeyboardLayer.EMOJI,
                touchExplorationEnabled = false,
            ),
        )
        assertFalse(
            SpacebarCursorAvailabilityPolicy.isEnabled(
                settingEnabled = true,
                sensitiveInput = false,
                layer = KeyboardLayer.LETTERS,
                touchExplorationEnabled = true,
            ),
        )
    }
}
