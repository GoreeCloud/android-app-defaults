package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryAnimatedThumbnailPolicyTest {
    @Test
    fun `only GIF image MIME types use animated thumbnail decoding`() {
        assertTrue(GalleryAnimatedThumbnailPolicy.isAnimatedGif("image/gif"))
        assertTrue(GalleryAnimatedThumbnailPolicy.isAnimatedGif(" IMAGE/GIF "))
        assertFalse(GalleryAnimatedThumbnailPolicy.isAnimatedGif("image/jpeg"))
        assertFalse(GalleryAnimatedThumbnailPolicy.isAnimatedGif("video/gif"))
    }

    @Test
    fun `target sizing preserves aspect ratio and never upscales`() {
        assertEquals(
            256 to 128,
            GalleryAnimatedThumbnailPolicy.targetSize(4000, 2000, 256),
        )
        assertEquals(
            120 to 60,
            GalleryAnimatedThumbnailPolicy.targetSize(120, 60, 256),
        )
    }

    @Test
    fun `target sizing caps requested decode edge and rejects invalid dimensions`() {
        assertEquals(
            GalleryAnimatedThumbnailPolicy.MAX_TARGET_EDGE_PX to 256,
            GalleryAnimatedThumbnailPolicy.targetSize(4000, 2000, 4096),
        )
        assertNull(GalleryAnimatedThumbnailPolicy.targetSize(0, 100, 128))
        assertNull(GalleryAnimatedThumbnailPolicy.targetSize(100, 100, 0))
    }
}
