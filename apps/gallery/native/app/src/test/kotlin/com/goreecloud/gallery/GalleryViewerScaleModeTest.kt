package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryViewerScaleModeTest {
    @Test
    fun `fit and fill toggle deterministically`() {
        assertEquals(GalleryViewerScaleMode.FILL, GalleryViewerScaleMode.FIT.next())
        assertEquals(GalleryViewerScaleMode.FIT, GalleryViewerScaleMode.FILL.next())
    }
}
