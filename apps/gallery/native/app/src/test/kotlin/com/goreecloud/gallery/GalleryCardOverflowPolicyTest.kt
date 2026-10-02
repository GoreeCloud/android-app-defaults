package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryCardOverflowPolicyTest {
    @Test
    fun `album overflow exposes app local organization without media mutation`() {
        assertEquals(
            listOf(
                GalleryCardOverflowAction.OPEN,
                GalleryCardOverflowAction.PIN_TO_TOP,
                GalleryCardOverflowAction.MOVE_LATER,
                GalleryCardOverflowAction.DETAILS,
            ),
            GalleryCardOverflowPolicy.albumActions(
                isPinned = false,
                canPin = true,
                canMoveEarlier = false,
                canMoveLater = true,
            ),
        )
        assertEquals(
            listOf(
                GalleryCardOverflowAction.OPEN,
                GalleryCardOverflowAction.UNPIN_FROM_TOP,
                GalleryCardOverflowAction.MOVE_EARLIER,
                GalleryCardOverflowAction.MOVE_LATER,
                GalleryCardOverflowAction.DETAILS,
            ),
            GalleryCardOverflowPolicy.albumActions(
                isPinned = true,
                canPin = true,
                canMoveEarlier = true,
                canMoveLater = true,
            ),
        )
        assertEquals(
            listOf(
                GalleryCardOverflowAction.OPEN,
                GalleryCardOverflowAction.DETAILS,
            ),
            GalleryCardOverflowPolicy.albumActions(
                isPinned = false,
                canPin = false,
                canMoveEarlier = false,
                canMoveLater = false,
            ),
        )
    }

    @Test
    fun `video overflow exposes share favorite and details only`() {
        assertEquals(
            listOf(
                GalleryCardOverflowAction.SHARE,
                GalleryCardOverflowAction.ADD_FAVORITE,
                GalleryCardOverflowAction.DETAILS,
            ),
            GalleryCardOverflowPolicy.videoActions(isFavorite = false),
        )
        assertEquals(
            listOf(
                GalleryCardOverflowAction.SHARE,
                GalleryCardOverflowAction.REMOVE_FAVORITE,
                GalleryCardOverflowAction.DETAILS,
            ),
            GalleryCardOverflowPolicy.videoActions(isFavorite = true),
        )
    }
}
