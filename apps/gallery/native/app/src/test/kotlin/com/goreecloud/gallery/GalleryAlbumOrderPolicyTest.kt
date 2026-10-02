package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryAlbumOrderPolicyTest {
    @Test
    fun `manual order applies within pinned and unpinned sections`() {
        assertEquals(
            listOf("screens", "camera", "family", "downloads"),
            GalleryAlbumOrderPolicy.orderedIds(
                availableAlbumIds = listOf("camera", "downloads", "screens", "family"),
                pinnedAlbumIds = setOf("camera", "screens"),
                manualOrderIds = listOf("screens", "family", "camera", "downloads"),
            ),
        )
    }

    @Test
    fun `unknown stored ids are ignored while new albums append from current base order`() {
        assertEquals(
            listOf("downloads", "camera", "family"),
            GalleryAlbumOrderPolicy.orderedIds(
                availableAlbumIds = listOf("camera", "downloads", "family"),
                pinnedAlbumIds = emptySet(),
                manualOrderIds = listOf("missing", "downloads", "camera"),
            ),
        )
    }

    @Test
    fun `move earlier and later never cross the pin boundary`() {
        val available = listOf("camera", "screens", "downloads", "family")
        val pinned = setOf("camera", "screens")

        assertEquals(
            listOf("screens", "camera", "downloads", "family"),
            GalleryAlbumOrderPolicy.movedOrder(
                availableAlbumIds = available,
                pinnedAlbumIds = pinned,
                manualOrderIds = emptyList(),
                albumId = "screens",
                direction = GalleryAlbumMoveDirection.EARLIER,
            ),
        )
        assertEquals(
            listOf("camera", "screens", "family", "downloads"),
            GalleryAlbumOrderPolicy.movedOrder(
                availableAlbumIds = available,
                pinnedAlbumIds = pinned,
                manualOrderIds = emptyList(),
                albumId = "family",
                direction = GalleryAlbumMoveDirection.EARLIER,
            ),
        )
        assertFalse(
            GalleryAlbumOrderPolicy.canMove(
                available,
                pinned,
                emptyList(),
                "camera",
                GalleryAlbumMoveDirection.EARLIER,
            ),
        )
        assertFalse(
            GalleryAlbumOrderPolicy.canMove(
                available,
                pinned,
                emptyList(),
                "screens",
                GalleryAlbumMoveDirection.LATER,
            ),
        )
    }

    @Test
    fun `moving an active album preserves temporarily unavailable stored ids`() {
        assertEquals(
            listOf("downloads", "camera", "temporarily-hidden"),
            GalleryAlbumOrderPolicy.movedOrder(
                availableAlbumIds = listOf("camera", "downloads"),
                pinnedAlbumIds = emptySet(),
                manualOrderIds = listOf("camera", "temporarily-hidden", "downloads"),
                albumId = "downloads",
                direction = GalleryAlbumMoveDirection.EARLIER,
            ),
        )
    }

    @Test
    fun `move availability follows the current effective section order`() {
        val available = listOf("camera", "downloads", "screens")

        assertTrue(
            GalleryAlbumOrderPolicy.canMove(
                available,
                emptySet(),
                listOf("screens", "camera", "downloads"),
                "camera",
                GalleryAlbumMoveDirection.EARLIER,
            ),
        )
        assertTrue(
            GalleryAlbumOrderPolicy.canMove(
                available,
                emptySet(),
                listOf("screens", "camera", "downloads"),
                "camera",
                GalleryAlbumMoveDirection.LATER,
            ),
        )
    }
}
