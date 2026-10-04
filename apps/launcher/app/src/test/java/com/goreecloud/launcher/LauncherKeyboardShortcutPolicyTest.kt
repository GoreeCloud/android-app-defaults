package com.goreecloud.launcher

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun modifiedNavigationShortcutsMapToExistingLauncherSurfaces() {
        assertEquals(
            LauncherKeyboardNavigationAction.HOME,
            LauncherKeyboardShortcutPolicy.navigationAction(
                keyCode = KeyEvent.KEYCODE_H,
                ctrlPressed = true,
                metaPressed = false,
                shiftPressed = true,
                altPressed = false,
            ),
        )
        assertEquals(
            LauncherKeyboardNavigationAction.APPS,
            LauncherKeyboardShortcutPolicy.navigationAction(
                keyCode = KeyEvent.KEYCODE_A,
                ctrlPressed = false,
                metaPressed = true,
                shiftPressed = true,
                altPressed = false,
            ),
        )
        assertEquals(
            LauncherKeyboardNavigationAction.SETTINGS,
            LauncherKeyboardShortcutPolicy.navigationAction(
                keyCode = KeyEvent.KEYCODE_COMMA,
                ctrlPressed = true,
                metaPressed = false,
                shiftPressed = false,
                altPressed = false,
            ),
        )
    }

    @Test
    fun navigationShortcutsRejectMissingModifiersAndAltChords() {
        assertNull(
            LauncherKeyboardShortcutPolicy.navigationAction(
                keyCode = KeyEvent.KEYCODE_A,
                ctrlPressed = false,
                metaPressed = false,
                shiftPressed = true,
                altPressed = false,
            ),
        )
        assertNull(
            LauncherKeyboardShortcutPolicy.navigationAction(
                keyCode = KeyEvent.KEYCODE_COMMA,
                ctrlPressed = true,
                metaPressed = false,
                shiftPressed = false,
                altPressed = true,
            ),
        )
    }

}
