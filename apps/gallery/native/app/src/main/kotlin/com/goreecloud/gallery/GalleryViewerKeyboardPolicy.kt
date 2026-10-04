package com.goreecloud.gallery

import android.view.KeyEvent

enum class GalleryViewerKeyboardAction {
    PREVIOUS,
    NEXT,
    TOGGLE_PLAYBACK,
    CLOSE,
}

object GalleryViewerKeyboardPolicy {
    fun actionFor(
        keyCode: Int,
        ctrlPressed: Boolean,
        metaPressed: Boolean,
        altPressed: Boolean,
    ): GalleryViewerKeyboardAction? {
        if (ctrlPressed || metaPressed || altPressed) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_PAGE_UP,
            -> GalleryViewerKeyboardAction.PREVIOUS
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_PAGE_DOWN,
            -> GalleryViewerKeyboardAction.NEXT
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            -> GalleryViewerKeyboardAction.TOGGLE_PLAYBACK
            KeyEvent.KEYCODE_ESCAPE -> GalleryViewerKeyboardAction.CLOSE
            else -> null
        }
    }
}
