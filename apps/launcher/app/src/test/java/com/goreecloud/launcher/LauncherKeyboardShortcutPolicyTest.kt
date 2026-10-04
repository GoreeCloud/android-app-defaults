package com.goreecloud.launcher

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun controlOrMetaFAlsoOpensUniversalSearch() {
        assertTrue(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_F,
                ctrlPressed = true,
                metaPressed = false,
                altPressed = false,
            ),
        )
        assertTrue(
            LauncherKeyboardShortcutPolicy.opensUniversalSearch(
                keyCode = KeyEvent.KEYCODE_F,
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
