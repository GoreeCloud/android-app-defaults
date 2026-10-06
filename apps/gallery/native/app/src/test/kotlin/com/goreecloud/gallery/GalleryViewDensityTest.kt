package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryViewDensityTest {
    @Test
    fun `dense preserves the adaptive Gallery grid baseline`() {
        assertEquals(3, GalleryViewDensity.DENSE.mediaGridColumns(390))
        assertEquals(5, GalleryViewDensity.DENSE.mediaGridColumns(820))
        assertEquals(8, GalleryViewDensity.DENSE.mediaGridColumns(1280))
    }

    @Test
    fun `comfortable reduces density without dropping below two columns`() {
        assertEquals(2, GalleryViewDensity.COMFORTABLE.mediaGridColumns(320))
        assertEquals(2, GalleryViewDensity.COMFORTABLE.mediaGridColumns(390))
        assertEquals(4, GalleryViewDensity.COMFORTABLE.mediaGridColumns(820))
        assertEquals(7, GalleryViewDensity.COMFORTABLE.mediaGridColumns(1280))
    }

    @Test
    fun `unknown stored values fail back to current dense behavior`() {
        assertEquals(GalleryViewDensity.DENSE, GalleryViewDensity.fromStored("future-value"))
        assertEquals(GalleryViewDensity.COMFORTABLE, GalleryViewDensity.fromStored("comfortable"))
    }
}
