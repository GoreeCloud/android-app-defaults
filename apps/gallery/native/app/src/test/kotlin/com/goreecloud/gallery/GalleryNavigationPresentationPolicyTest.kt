package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryNavigationPresentationPolicyTest {
    @Test
    fun `icons only uses a truly centered glyph instead of a top compound drawable`() {
        assertEquals(
            GalleryNavigationIconPlacement.CENTERED,
            GalleryNavigationPresentationPolicy.iconPlacement(
                GalleryNavigationDisplayMode.ICONS_ONLY,
            ),
        )
    }

    @Test
    fun `combined mode keeps glyph above its visible label`() {
        assertEquals(
            GalleryNavigationIconPlacement.ABOVE_LABEL,
            GalleryNavigationPresentationPolicy.iconPlacement(
                GalleryNavigationDisplayMode.ICONS_AND_TEXT,
            ),
        )
    }

    @Test
    fun `text only removes the glyph presentation`() {
        assertEquals(
            GalleryNavigationIconPlacement.NONE,
            GalleryNavigationPresentationPolicy.iconPlacement(
                GalleryNavigationDisplayMode.TEXT_ONLY,
            ),
        )
    }

    @Test
    fun `icons only uses tighter selected material than labeled modes`() {
        val iconsInset = GalleryNavigationPresentationPolicy.selectedHorizontalInsetDp(
            GalleryNavigationDisplayMode.ICONS_ONLY,
        )
        val labeledInset = GalleryNavigationPresentationPolicy.selectedHorizontalInsetDp(
            GalleryNavigationDisplayMode.ICONS_AND_TEXT,
        )

        assertEquals(true, iconsInset > labeledInset)
    }
}
