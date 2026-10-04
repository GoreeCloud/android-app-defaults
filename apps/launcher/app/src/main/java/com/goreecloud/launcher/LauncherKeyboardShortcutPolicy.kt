package com.goreecloud.launcher

import android.view.KeyEvent

/**
 * Bounded hardware-keyboard entry points for Launcher-owned Universal Search.
 *
 * The policy does not launch providers or bypass source controls; it only requests the existing
 * Launcher Search surface. Alt-modified chords are left to Android/app conventions.
 */
enum class LauncherKeyboardNavigationAction {
    HOME,
    APPS,
    SETTINGS,
}

data class LauncherKeyboardShortcutRequest(
    val sequence: Long = 0L,
    val action: LauncherKeyboardNavigationAction? = null,
)

object LauncherKeyboardShortcutPolicy {
    fun opensUniversalSearch(
        keyCode: Int,
        ctrlPressed: Boolean,
        metaPressed: Boolean,
        altPressed: Boolean,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) return true
        if (altPressed) return false
        return (keyCode == KeyEvent.KEYCODE_K || keyCode == KeyEvent.KEYCODE_F) &&
            (ctrlPressed || metaPressed)
    }

    fun navigationAction(
        keyCode: Int,
        ctrlPressed: Boolean,
        metaPressed: Boolean,
        shiftPressed: Boolean,
        altPressed: Boolean,
    ): LauncherKeyboardNavigationAction? {
        if (altPressed || (!ctrlPressed && !metaPressed)) return null
        return when {
            shiftPressed && keyCode == KeyEvent.KEYCODE_H ->
                LauncherKeyboardNavigationAction.HOME
            shiftPressed && keyCode == KeyEvent.KEYCODE_A ->
                LauncherKeyboardNavigationAction.APPS
            !shiftPressed && keyCode == KeyEvent.KEYCODE_COMMA ->
                LauncherKeyboardNavigationAction.SETTINGS
            else -> null
        }
    }

}
