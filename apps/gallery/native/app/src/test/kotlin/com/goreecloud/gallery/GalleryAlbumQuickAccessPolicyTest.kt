package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryAlbumQuickAccessPolicyTest {
    @Test
    fun `favorites always receive the first quick-access slot`() {
        assertEquals(0, GalleryAlbumQuickAccessPolicy.priority("Anything", isFavorites = true))
    }

    @Test
    fun `common device collections are recognized case insensitively`() {
        assertEquals(1, GalleryAlbumQuickAccessPolicy.priority("Camera", isFavorites = false))
        assertEquals(2, GalleryAlbumQuickAccessPolicy.priority("Screenshots", isFavorites = false))
        assertEquals(3, GalleryAlbumQuickAccessPolicy.priority("DOWNLOADS", isFavorites = false))
        assertEquals(4, GalleryAlbumQuickAccessPolicy.priority("Screen recordings", isFavorites = false))
    }

    @Test
    fun `ordinary albums do not become synthetic quick-access categories`() {
        assertNull(GalleryAlbumQuickAccessPolicy.priority("Family vacation", isFavorites = false))
        assertNull(GalleryAlbumQuickAccessPolicy.priority("Receipts", isFavorites = false))
    }
}
