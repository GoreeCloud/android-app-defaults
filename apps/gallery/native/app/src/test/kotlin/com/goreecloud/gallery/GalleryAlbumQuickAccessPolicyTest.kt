package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryAlbumQuickAccessPolicyTest {
    @Test
    fun `known quick-access collections expose stable semantic kinds`() {
        assertEquals(
            GalleryAlbumQuickAccessKind.FAVORITES,
            GalleryAlbumQuickAccessPolicy.kind("Anything", isFavorites = true),
        )
        assertEquals(
            GalleryAlbumQuickAccessKind.CAMERA,
            GalleryAlbumQuickAccessPolicy.kind("Camera", isFavorites = false),
        )
        assertEquals(
            GalleryAlbumQuickAccessKind.SCREENSHOTS,
            GalleryAlbumQuickAccessPolicy.kind("Screenshots", isFavorites = false),
        )
        assertEquals(
            GalleryAlbumQuickAccessKind.DOWNLOADS,
            GalleryAlbumQuickAccessPolicy.kind("Downloads", isFavorites = false),
        )
        assertEquals(
            GalleryAlbumQuickAccessKind.SCREEN_RECORDINGS,
            GalleryAlbumQuickAccessPolicy.kind("Screen recordings", isFavorites = false),
        )
        assertNull(
            GalleryAlbumQuickAccessPolicy.kind("Family vacation", isFavorites = false),
        )
    }

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
