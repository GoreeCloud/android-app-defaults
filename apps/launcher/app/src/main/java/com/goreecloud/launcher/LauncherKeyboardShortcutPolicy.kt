package com.goreecloud.launcher

import android.view.KeyEvent

/**
 * Bounded hardware-keyboard entry points for Launcher-owned Universal Search.
 *
 * The policy does not launch providers or bypass source controls; it only requests the existing
 * Launcher Search surface. Alt-modified chords are left to Android/app conventions.
 */
object LauncherKeyboardShortcutPolicy {
    fun opensUniversalSearch(
        keyCode: Int,
        ctrlPressed: Boolean,
        metaPressed: Boolean,
        altPressed: Boolean,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) return true
        if (altPressed) return false
        return keyCode == KeyEvent.KEYCODE_K && (ctrlPressed || metaPressed)
    }
}
