package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryMediaAccessPolicyTest {
    @Test
    fun android14SelectedAccessIsPartialRatherThanFull() {
        val scope = GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(
                apiLevel = 34,
                readMediaVisualUserSelected = true,
            ),
        )

        assertEquals(GalleryMediaAccessScope.SELECTED, scope)
        assertTrue(GalleryMediaAccessPolicy.canRead(scope))
        assertTrue(GalleryMediaAccessPolicy.isPartial(scope))
    }

    @Test
    fun fullTypeGrantsTakePrecedenceOverSelectedMarker() {
        val imagesOnly = GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(
                apiLevel = 36,
                readMediaImages = true,
                readMediaVisualUserSelected = true,
            ),
        )
        val imagesAndVideos = GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(
                apiLevel = 36,
                readMediaImages = true,
                readMediaVideo = true,
                readMediaVisualUserSelected = true,
            ),
        )

        assertEquals(GalleryMediaAccessScope.IMAGES, imagesOnly)
        assertTrue(GalleryMediaAccessPolicy.isPartial(imagesOnly))
        assertEquals(GalleryMediaAccessScope.IMAGES_AND_VIDEOS, imagesAndVideos)
        assertFalse(GalleryMediaAccessPolicy.isPartial(imagesAndVideos))
    }

    @Test
    fun android13TracksImageAndVideoAuthoritySeparatelyAsPartialGalleryAccess() {
        val images = GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(apiLevel = 33, readMediaImages = true),
        )
        val videos = GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(apiLevel = 33, readMediaVideo = true),
        )

        assertEquals(GalleryMediaAccessScope.IMAGES, images)
        assertTrue(GalleryMediaAccessPolicy.canRead(images))
        assertTrue(GalleryMediaAccessPolicy.isPartial(images))
        assertEquals(GalleryMediaAccessScope.VIDEOS, videos)
        assertTrue(GalleryMediaAccessPolicy.canRead(videos))
        assertTrue(GalleryMediaAccessPolicy.isPartial(videos))
    }

    @Test
    fun legacyReadExternalStorageIsFullOnlyThroughApi32() {
        val legacy = GalleryMediaAccessPolicy.resolve(
            GalleryMediaPermissionSnapshot(apiLevel = 32, readExternalStorage = true),
        )
        assertEquals(GalleryMediaAccessScope.LEGACY_FULL, legacy)
        assertFalse(GalleryMediaAccessPolicy.isPartial(legacy))
        assertEquals(
            GalleryMediaAccessScope.DENIED,
            GalleryMediaAccessPolicy.resolve(
                GalleryMediaPermissionSnapshot(apiLevel = 33, readExternalStorage = true),
            ),
        )
    }

    @Test
    fun missingAuthorityFailsClosed() {
        val scope = GalleryMediaAccessPolicy.resolve(GalleryMediaPermissionSnapshot(apiLevel = 36))
        assertEquals(GalleryMediaAccessScope.DENIED, scope)
        assertFalse(GalleryMediaAccessPolicy.canRead(scope))
        assertFalse(GalleryMediaAccessPolicy.isPartial(scope))
    }
}
