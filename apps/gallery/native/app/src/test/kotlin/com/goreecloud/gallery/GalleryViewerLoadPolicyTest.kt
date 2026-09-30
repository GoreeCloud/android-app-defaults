package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryViewerLoadPolicyTest {
    @Test
    fun `authorized image with measured viewport prefers orientation-aware decoder`() {
        assertEquals(
            GalleryViewerLoadPath.ORIENTATION_AWARE_IMAGE,
            GalleryViewerLoadPolicy.primaryPath(
                mimeType = "image/jpeg",
                viewportWidth = 1080,
                viewportHeight = 2224,
            ),
        )
    }

    @Test
    fun `video retains thumbnail poster loading`() {
        assertEquals(
            GalleryViewerLoadPath.THUMBNAIL,
            GalleryViewerLoadPolicy.primaryPath(
                mimeType = "video/mp4",
                viewportWidth = 1080,
                viewportHeight = 2224,
            ),
        )
    }

    @Test
    fun `unknown media retains thumbnail loading`() {
        assertEquals(
            GalleryViewerLoadPath.THUMBNAIL,
            GalleryViewerLoadPolicy.primaryPath(
                mimeType = "application/octet-stream",
                viewportWidth = 1080,
                viewportHeight = 2224,
            ),
        )
    }

    @Test
    fun `image before viewport measurement fails closed to thumbnail path`() {
        assertEquals(
            GalleryViewerLoadPath.THUMBNAIL,
            GalleryViewerLoadPolicy.primaryPath(
                mimeType = "image/heic",
                viewportWidth = 0,
                viewportHeight = 2224,
            ),
        )
        assertEquals(
            GalleryViewerLoadPath.THUMBNAIL,
            GalleryViewerLoadPolicy.primaryPath(
                mimeType = "image/png",
                viewportWidth = 1080,
                viewportHeight = 0,
            ),
        )
    }
}
