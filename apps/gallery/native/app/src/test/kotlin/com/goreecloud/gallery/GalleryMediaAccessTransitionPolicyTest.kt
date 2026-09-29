package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryMediaAccessTransitionPolicyTest {
    @Test
    fun `first observed scope establishes baseline without recreation`() {
        assertFalse(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = null,
                current = GalleryMediaAccessScope.SELECTED,
            ),
        )
    }

    @Test
    fun `unchanged non-selected authority keeps current presentation`() {
        GalleryMediaAccessScope.entries
            .filterNot { it == GalleryMediaAccessScope.SELECTED }
            .forEach { scope ->
                assertFalse(
                    GalleryMediaAccessTransitionPolicy.requiresPresentationReset(scope, scope),
                    "unchanged $scope scope should not reset presentation",
                )
            }
    }

    @Test
    fun `repeated selected-media resume invalidates presentation`() {
        assertTrue(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = GalleryMediaAccessScope.SELECTED,
                current = GalleryMediaAccessScope.SELECTED,
            ),
        )
    }

    @Test
    fun `full to partial authority invalidates previously readable presentation`() {
        assertTrue(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = GalleryMediaAccessScope.IMAGES_AND_VIDEOS,
                current = GalleryMediaAccessScope.SELECTED,
            ),
        )
        assertTrue(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = GalleryMediaAccessScope.IMAGES_AND_VIDEOS,
                current = GalleryMediaAccessScope.IMAGES,
            ),
        )
    }

    @Test
    fun `authority revocation invalidates presentation`() {
        assertTrue(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = GalleryMediaAccessScope.VIDEOS,
                current = GalleryMediaAccessScope.DENIED,
            ),
        )
    }

    @Test
    fun `regained or broadened authority also forces fresh MediaStore query`() {
        assertTrue(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = GalleryMediaAccessScope.DENIED,
                current = GalleryMediaAccessScope.IMAGES,
            ),
        )
        assertTrue(
            GalleryMediaAccessTransitionPolicy.requiresPresentationReset(
                previous = GalleryMediaAccessScope.SELECTED,
                current = GalleryMediaAccessScope.IMAGES_AND_VIDEOS,
            ),
        )
    }
}
