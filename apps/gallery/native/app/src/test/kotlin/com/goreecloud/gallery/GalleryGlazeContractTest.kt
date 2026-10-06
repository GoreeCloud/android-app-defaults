package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryGlazeContractTest {
    @Test
    fun `native shell pins bounded current Glaze V1_7 anchor`() {
        assertEquals("1.7.0", GalleryGlazeContract.VERSION)
        assertEquals("Glaze V1.7", GalleryGlazeContract.PRODUCT_LABEL)
        assertEquals(
            "1a5756daed2294155be2e9972b24f580f6222b7b",
            GalleryGlazeContract.SHARED_RELEASE_INTEGRATION,
        )
        assertEquals(
            "7c4ded83d7a8725165bb6a55dfb175667cc9589e",
            GalleryGlazeContract.SOURCE_QUALIFICATION_ANCHOR,
        )
        assertEquals("js/glaze-v1.7.0.mjs", GalleryGlazeContract.STABLE_RUNTIME_ENTRYPOINT)
        assertEquals("js/glaze-v1.6.0.mjs", GalleryGlazeContract.INHERITED_RUNTIME_ENTRYPOINT)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GalleryGlazeContract.INHERITED_ACCEPTED_RELEASE_SOURCE,
        )
        assertEquals("1.6.0", GalleryGlazeContract.ROLLBACK_BASELINE)
        assertFalse(GalleryGlazeContract.RETAINED_DEVELOPMENT_SOURCE_INCLUDED)
        assertFalse(GalleryGlazeContract.SECTION_48_INCLUDED)
    }

    @Test
    fun `Glaze authority remains presentation only and fail closed`() {
        assertTrue(GalleryGlazeContract.PRESENTATION_ONLY)
        assertFalse(GalleryGlazeContract.PERMISSION_REQUEST_AUTOMATIC)
        assertFalse(GalleryGlazeContract.AUTHORIZATION_INFERRED)
        assertFalse(GalleryGlazeContract.CONSEQUENTIAL_EXECUTION_AUTOMATIC)
        assertFalse(GalleryGlazeContract.DOWNSTREAM_CONSUMER_ACCEPTANCE_AUTOMATIC)
    }

    @Test
    fun `Gallery optical adapter remains bounded and accessibility safe`() {
        assertTrue(GalleryGlazeContract.OPTICAL_CONTENT_AWARE_FROST_ENABLED)
        assertTrue(GalleryGlazeContract.OPTICAL_SEMANTIC_BLUR_PROTECTION_ENABLED)
        assertTrue(GalleryGlazeContract.OPTICAL_REDUCED_TRANSPARENCY_FALLBACK_REQUIRED)
        assertTrue(GalleryGlazeContract.OPTICAL_INCREASED_CONTRAST_FALLBACK_REQUIRED)
        assertTrue(GalleryGlazeContract.OPTICAL_MEMORY_TINT_MAX_FRACTION in 0f..0.08f)
        assertFalse(GalleryGlazeContract.OPTICAL_ENVIRONMENT_TINT_MAY_OVERRIDE_SEMANTIC_STATE)
    }

    @Test
    fun `adaptive layout exposes compact medium and expanded classes`() {
        assertEquals(GalleryGlazeContract.WidthClass.COMPACT, GalleryGlazeContract.widthClass(390))
        assertEquals(GalleryGlazeContract.WidthClass.MEDIUM, GalleryGlazeContract.widthClass(700))
        assertEquals(GalleryGlazeContract.WidthClass.EXPANDED, GalleryGlazeContract.widthClass(900))
        assertEquals(12, GalleryGlazeContract.horizontalGutterDp(390))
        assertEquals(24, GalleryGlazeContract.horizontalGutterDp(700))
        assertEquals(32, GalleryGlazeContract.horizontalGutterDp(900))
        assertTrue(GalleryGlazeContract.CONTENT_MAX_WIDTH_DP >= 1200)
        assertTrue(GalleryGlazeContract.SETTINGS_MAX_WIDTH_DP <= GalleryGlazeContract.CONTENT_MAX_WIDTH_DP)
    }

    @Test
    fun `photo grid favors larger phone thumbnails and scales wider`() {
        assertEquals(3, GalleryGlazeContract.gridColumns(320))
        assertEquals(3, GalleryGlazeContract.gridColumns(390))
        assertEquals(4, GalleryGlazeContract.gridColumns(430))
        assertEquals(5, GalleryGlazeContract.gridColumns(700))
        assertEquals(6, GalleryGlazeContract.gridColumns(900))
        assertEquals(8, GalleryGlazeContract.gridColumns(1400))
        assertTrue(GalleryGlazeContract.MIN_GRID_TILE_DP >= 92)
    }

    @Test
    fun `album and video grids retain hierarchy across width classes`() {
        assertEquals(2, GalleryGlazeContract.albumGridColumns(390))
        assertEquals(3, GalleryGlazeContract.albumGridColumns(700))
        assertEquals(4, GalleryGlazeContract.albumGridColumns(900))
        assertEquals(5, GalleryGlazeContract.albumGridColumns(1280))
        assertEquals(2, GalleryGlazeContract.videoGridColumns(390))
        assertEquals(2, GalleryGlazeContract.videoGridColumns(700))
        assertEquals(3, GalleryGlazeContract.videoGridColumns(900))
        assertEquals(4, GalleryGlazeContract.videoGridColumns(1280))
        assertFalse(GalleryGlazeContract.videoUsesFeaturedCard(700))
        assertTrue(GalleryGlazeContract.videoUsesFeaturedCard(720))
    }

    @Test
    fun `Trash grid remains legible on phones before scaling wider`() {
        assertEquals(3, GalleryGlazeContract.trashGridColumns(320))
        assertEquals(3, GalleryGlazeContract.trashGridColumns(390))
        assertEquals(4, GalleryGlazeContract.trashGridColumns(600))
        assertEquals(5, GalleryGlazeContract.trashGridColumns(900))
        assertEquals(6, GalleryGlazeContract.trashGridColumns(1280))
    }

    @Test
    fun `adaptive navigation stays glyph first touch safe and gesture aware`() {
        assertEquals(56, GalleryGlazeContract.NAVIGATION_HEIGHT_DP)
        assertEquals(18, GalleryGlazeContract.NAVIGATION_RADIUS_DP)
        assertEquals(10, GalleryGlazeContract.NAVIGATION_SIDE_MARGIN_DP)
        assertEquals(8, GalleryGlazeContract.NAVIGATION_BOTTOM_MARGIN_DP)
        assertEquals(72, GalleryGlazeContract.NAVIGATION_RESERVED_SPACE_DP)
        assertEquals(22, GalleryGlazeContract.NAVIGATION_ICON_DP)
        assertEquals(64, GalleryGlazeContract.NAVIGATION_RAIL_WIDTH_DP)
        assertEquals(52, GalleryGlazeContract.NAVIGATION_RAIL_ITEM_HEIGHT_DP)
        assertTrue(GalleryGlazeContract.NAVIGATION_HEIGHT_DP >= GalleryGlazeContract.GENERAL_TARGET_DP)
        assertTrue(GalleryGlazeContract.NAVIGATION_RAIL_ITEM_HEIGHT_DP >= GalleryGlazeContract.GENERAL_TARGET_DP)
        assertTrue(
            GalleryGlazeContract.NAVIGATION_RESERVED_SPACE_DP >=
                GalleryGlazeContract.NAVIGATION_HEIGHT_DP + GalleryGlazeContract.NAVIGATION_BOTTOM_MARGIN_DP,
        )
        assertFalse(GalleryGlazeContract.usesNavigationRail(700))
        assertTrue(GalleryGlazeContract.usesNavigationRail(900))
        assertEquals(88, GalleryGlazeContract.navigationRailLaneDp(900))
        assertEquals(0, GalleryGlazeContract.navigationRailLaneDp(700))
        assertTrue(GalleryGlazeContract.MaterialRole.entries.contains(GalleryGlazeContract.MaterialRole.CANVAS))
        assertTrue(
            GalleryGlazeContract.MaterialRole.entries.contains(
                GalleryGlazeContract.MaterialRole.FUNCTIONAL_GLASS,
            ),
        )
    }

    @Test
    fun `rendered local library remains bounded`() {
        assertEquals(100, GalleryGlazeContract.MAX_RENDERED_MEDIA_ROWS)
    }
}
