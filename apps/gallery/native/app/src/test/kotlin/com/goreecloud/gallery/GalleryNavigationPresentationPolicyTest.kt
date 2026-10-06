package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GalleryNavigationPresentationPolicyTest {
    @Test
    fun `all legacy display modes resolve to centered glyph-only navigation`() {
        GalleryNavigationDisplayMode.entries.forEach { mode ->
            assertEquals(
                GalleryNavigationIconPlacement.CENTERED,
                GalleryNavigationPresentationPolicy.iconPlacement(mode),
            )
            assertFalse(GalleryNavigationPresentationPolicy.showsVisibleLabel(mode))
        }
    }

    @Test
    fun `selection material stays equally compact for every legacy mode`() {
        GalleryNavigationDisplayMode.entries.forEach { mode ->
            assertEquals(
                GalleryGlazeContract.NAVIGATION_SELECTED_HORIZONTAL_INSET_DP,
                GalleryNavigationPresentationPolicy.selectedHorizontalInsetDp(mode),
            )
            assertEquals(
                GalleryGlazeContract.NAVIGATION_SELECTED_VERTICAL_INSET_DP,
                GalleryNavigationPresentationPolicy.selectedVerticalInsetDp(mode),
            )
        }
    }
}
