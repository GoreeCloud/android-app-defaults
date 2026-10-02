package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryAlbumOrderPolicyTest {
    @Test
    fun `preferred order applies within pinned and unpinned groups`() {
        assertEquals(
            listOf("screens", "camera", "family", "downloads"),
            GalleryAlbumOrderPolicy.orderedIds(
                availableAlbumIds = listOf("camera", "downloads", "screens", "family"),
                pinnedAlbumIds = setOf("camera", "screens"),
                preferredAlbumOrderIds = listOf("family", "screens", "camera", "downloads"),
            ),
        )
    }

    @Test
    fun `new albums fall back to default order and stale ids create no collections`() {
        assertEquals(
            listOf("camera", "downloads", "family"),
            GalleryAlbumOrderPolicy.orderedIds(
                availableAlbumIds = listOf("camera", "downloads", "family"),
                pinnedAlbumIds = emptySet(),
                preferredAlbumOrderIds = listOf("camera", "missing", "downloads"),
            ),
        )
    }

    @Test
    fun `move earlier never crosses the pinned boundary`() {
        val availability = GalleryAlbumOrderPolicy.availability(
            availableAlbumIds = listOf("camera", "screens", "downloads", "family"),
            pinnedAlbumIds = setOf("camera", "screens"),
            preferredAlbumOrderIds = emptyList(),
            albumId = "downloads",
        )
        assertFalse(availability.canMoveEarlier)
        assertTrue(availability.canMoveLater)

        assertEquals(
            listOf("camera", "screens", "downloads", "family"),
            GalleryAlbumOrderPolicy.moved(
                availableAlbumIds = listOf("camera", "screens", "downloads", "family"),
                pinnedAlbumIds = setOf("camera", "screens"),
                preferredAlbumOrderIds = emptyList(),
                albumId = "downloads",
                direction = GalleryAlbumMoveDirection.EARLIER,
            ),
        )
    }

    @Test
    fun `move later swaps only within the current pin group and retains stale preferences`() {
        assertEquals(
            listOf("screens", "camera", "downloads", "family", "missing"),
            GalleryAlbumOrderPolicy.moved(
                availableAlbumIds = listOf("camera", "screens", "downloads", "family"),
                pinnedAlbumIds = setOf("camera", "screens"),
                preferredAlbumOrderIds = listOf("camera", "screens", "downloads", "family", "missing"),
                albumId = "camera",
                direction = GalleryAlbumMoveDirection.LATER,
            ),
        )
    }
}
