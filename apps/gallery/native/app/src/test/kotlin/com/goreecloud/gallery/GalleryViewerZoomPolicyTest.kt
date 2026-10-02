package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryViewerZoomPolicyTest {
    @Test
    fun `gesture scaling remains bounded between one and four times`() {
        assertEquals(1f, GalleryViewerZoomPolicy.scaleAfterGesture(1f, 0.2f))
        assertEquals(2f, GalleryViewerZoomPolicy.scaleAfterGesture(1f, 2f))
        assertEquals(4f, GalleryViewerZoomPolicy.scaleAfterGesture(3f, 3f))
        assertEquals(2f, GalleryViewerZoomPolicy.scaleAfterGesture(2f, Float.NaN))
    }

    @Test
    fun `translation is zero at baseline scale`() {
        assertEquals(
            GalleryViewerTranslation(0f, 0f),
            GalleryViewerZoomPolicy.boundedTranslation(
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 1f,
                proposedX = 90f,
                proposedY = -120f,
            ),
        )
    }

    @Test
    fun `translation stays inside scaled viewport bounds`() {
        assertEquals(
            GalleryViewerTranslation(200f, -400f),
            GalleryViewerZoomPolicy.boundedTranslation(
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 2f,
                proposedX = 900f,
                proposedY = -900f,
            ),
        )
        assertEquals(
            GalleryViewerTranslation(-50f, 75f),
            GalleryViewerZoomPolicy.boundedTranslation(
                viewportWidth = 400,
                viewportHeight = 800,
                scale = 3f,
                proposedX = -50f,
                proposedY = 75f,
            ),
        )
    }

    @Test
    fun `zoom state and accessible preset remain deterministic`() {
        assertFalse(GalleryViewerZoomPolicy.isZoomed(1f))
        assertTrue(GalleryViewerZoomPolicy.isZoomed(1.25f))
        assertTrue(GalleryViewerZoomPolicy.isPreset(2f))
        assertFalse(GalleryViewerZoomPolicy.isPreset(2.25f))
        assertEquals(250, GalleryViewerZoomPolicy.displayPercent(2.5f))
    }
}
