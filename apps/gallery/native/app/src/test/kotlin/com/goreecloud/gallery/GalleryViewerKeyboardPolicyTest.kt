package com.goreecloud.gallery

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryViewerKeyboardPolicyTest {
    @Test
    fun arrowsAndPagingNavigateViewer() {
        assertEquals(
            GalleryViewerKeyboardAction.PREVIOUS,
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_DPAD_LEFT,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
        assertEquals(
            GalleryViewerKeyboardAction.NEXT,
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_PAGE_DOWN,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
    }

    @Test
    fun playbackAndEscapeMapToExistingViewerActions() {
        assertEquals(
            GalleryViewerKeyboardAction.TOGGLE_PLAYBACK,
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_SPACE,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
        assertEquals(
            GalleryViewerKeyboardAction.CLOSE,
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_ESCAPE,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
    }

    @Test
    fun modifiedKeysRemainUnclaimed() {
        assertNull(
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_DPAD_RIGHT,
                ctrlPressed = true,
                metaPressed = false,
                altPressed = false,
            ),
        )
    }

    @Test
    fun fTogglesFavoriteOnlyWithoutModifiers() {
        assertEquals(
            GalleryViewerKeyboardAction.TOGGLE_FAVORITE,
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_F,
                ctrlPressed = false,
                metaPressed = false,
                altPressed = false,
            ),
        )
        assertNull(
            GalleryViewerKeyboardPolicy.actionFor(
                KeyEvent.KEYCODE_F,
                ctrlPressed = true,
                metaPressed = false,
                altPressed = false,
            ),
        )
    }

}
