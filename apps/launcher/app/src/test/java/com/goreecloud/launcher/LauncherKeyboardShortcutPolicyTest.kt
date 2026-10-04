package com.goreecloud.launcher

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LauncherKeyboardShortcutPolicyTest {
    @Test
    fun searchKeyAlwaysOpensUniversalSearch() {
        assertTrue(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_SEARCH,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
    }

    @Test
    fun controlOrMetaKOpensUniversalSearch() {
        assertTrue(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_K,
                ctrlPressed = true,
                metaPressed = false,
                altPressed = false,
            ),
        )
        assertTrue(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_K,
                ctrlPressed = false,
                metaPressed = true,
                altPressed = false,
            ),
        )
    }

    @Test
    fun ordinaryOrAltModifiedKIsNotCaptured() {
        assertFalse(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_K,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
        assertFalse(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_K,
                ctrlPressed = true,
                metaPressed = false,
                altPressed = true,
            ),
        )
    }
}
