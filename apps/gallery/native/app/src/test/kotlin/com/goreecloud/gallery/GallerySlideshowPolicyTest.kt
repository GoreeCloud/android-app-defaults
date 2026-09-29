package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GallerySlideshowPolicyTest {
    @Test
    fun `slideshow advances only to later photos and stops at the end`() {
        val photos = listOf(true, false, true, true)

        assertEquals(2, GallerySlideshowPolicy.nextPhotoIndex(0, photos))
        assertEquals(2, GallerySlideshowPolicy.nextPhotoIndex(1, photos))
        assertEquals(3, GallerySlideshowPolicy.nextPhotoIndex(2, photos))
        assertNull(GallerySlideshowPolicy.nextPhotoIndex(3, photos))
    }

    @Test
    fun `invalid current index fails closed`() {
        assertNull(GallerySlideshowPolicy.nextPhotoIndex(-1, listOf(true)))
        assertNull(GallerySlideshowPolicy.nextPhotoIndex(2, listOf(true)))
    }
}
