package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GalleryViewerDecodePolicyTest {
    @Test
    fun `image mime types use orientation-aware decoder path`() {
        assertTrue(GalleryViewerDecodePolicy.usesImageDecoder("image/jpeg"))
        assertTrue(GalleryViewerDecodePolicy.usesImageDecoder(" IMAGE/HEIC "))
        assertFalse(GalleryViewerDecodePolicy.usesImageDecoder("video/mp4"))
        assertFalse(GalleryViewerDecodePolicy.usesImageDecoder("application/octet-stream"))
    }

    @Test
    fun `landscape decode stays inside phone viewport without upscaling`() {
        assertEquals(
            GalleryViewerDecodeSize(width = 1080, height = 810),
            GalleryViewerDecodePolicy.boundedDecodeSize(
                sourceWidth = 4160,
                sourceHeight = 3120,
                viewportWidth = 1080,
                viewportHeight = 2400,
            ),
        )
    }

    @Test
    fun `portrait decode stays inside phone viewport without upscaling`() {
        assertEquals(
            GalleryViewerDecodeSize(width = 1080, height = 1440),
            GalleryViewerDecodePolicy.boundedDecodeSize(
                sourceWidth = 3120,
                sourceHeight = 4160,
                viewportWidth = 1080,
                viewportHeight = 2400,
            ),
        )
    }

    @Test
    fun `decode is capped at bounded long edge on large displays`() {
        assertEquals(
            GalleryViewerDecodeSize(width = 2048, height = 1365),
            GalleryViewerDecodePolicy.boundedDecodeSize(
                sourceWidth = 6000,
                sourceHeight = 4000,
                viewportWidth = 3000,
                viewportHeight = 3000,
            ),
        )
    }

    @Test
    fun `small images are never upscaled`() {
        assertEquals(
            GalleryViewerDecodeSize(width = 640, height = 480),
            GalleryViewerDecodePolicy.boundedDecodeSize(
                sourceWidth = 640,
                sourceHeight = 480,
                viewportWidth = 1080,
                viewportHeight = 2400,
            ),
        )
    }

    @Test
    fun `invalid dimensions fail closed`() {
        assertFailsWith<IllegalArgumentException> {
            GalleryViewerDecodePolicy.boundedDecodeSize(0, 100, 1080, 2400)
        }
        assertFailsWith<IllegalArgumentException> {
            GalleryViewerDecodePolicy.boundedDecodeSize(100, 100, 0, 2400)
        }
    }
}
