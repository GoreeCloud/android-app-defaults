package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryAlbumPinPolicyTest {
    @Test
    fun `pinned albums move to the front without changing relative order`() {
        assertEquals(
            listOf("camera", "screens", "downloads", "family"),
            GalleryAlbumPinPolicy.orderedIds(
                availableAlbumIds = listOf("camera", "downloads", "screens", "family"),
                pinnedAlbumIds = setOf("screens", "camera"),
            ),
        )
    }

    @Test
    fun `stale pins are ignored and duplicates fail closed to one visible album`() {
        assertEquals(
            listOf("camera", "downloads"),
            GalleryAlbumPinPolicy.orderedIds(
                availableAlbumIds = listOf("camera", "camera", "downloads"),
                pinnedAlbumIds = setOf("missing"),
            ),
        )
    }

    @Test
    fun `pin state toggles without disturbing unrelated pins`() {
        assertEquals(
            setOf("camera", "downloads"),
            GalleryAlbumPinPolicy.toggled(setOf("camera"), "downloads", pinned = true),
        )
        assertEquals(
            setOf("downloads"),
            GalleryAlbumPinPolicy.toggled(setOf("camera", "downloads"), "camera", pinned = false),
        )
    }
}
