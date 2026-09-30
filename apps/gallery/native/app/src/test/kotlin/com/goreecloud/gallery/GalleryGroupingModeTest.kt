package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals

class GalleryGroupingModeTest {
    @Test
    fun dayRemainsMigrationSafeDefault() {
        assertEquals(GalleryGroupingMode.DAY, GalleryGroupingMode.fromStored(null))
        assertEquals(GalleryGroupingMode.DAY, GalleryGroupingMode.fromStored("future-value"))
    }

    @Test
    fun yearGroupingHasStablePortableIdentity() {
        assertEquals("year", GalleryGroupingMode.YEAR.storedValue)
        assertEquals("Year", GalleryGroupingMode.YEAR.label)
        assertEquals(GalleryGroupingMode.YEAR, GalleryGroupingMode.fromStored("year"))
    }

    @Test
    fun groupingChoicesKeepBroadeningOrderBeforeContinuousGrid() {
        assertEquals(
            listOf(
                GalleryGroupingMode.DAY,
                GalleryGroupingMode.MONTH,
                GalleryGroupingMode.YEAR,
                GalleryGroupingMode.NONE,
            ),
            GalleryGroupingMode.entries,
        )
    }

    @Test
    fun storedGroupingModesRoundTrip() {
        GalleryGroupingMode.entries.forEach { mode ->
            assertEquals(mode, GalleryGroupingMode.fromStored(mode.storedValue))
        }
    }

    @Test
    fun sortPreferenceDefaultsToNewestAndRoundTripsStoredValues() {
        assertEquals(GallerySortPreference.NEWEST, GallerySortPreference.fromStored(null))
        assertEquals(GallerySortPreference.NEWEST, GallerySortPreference.fromStored("future-value"))
        GallerySortPreference.entries.forEach { preference ->
            assertEquals(preference, GallerySortPreference.fromStored(preference.storedValue))
        }
    }

    @Test
    fun sortPreferenceMapsToExistingCoreSortAuthority() {
        assertEquals(
            com.goreecloud.gallery.core.MediaSortOrder.NEWEST,
            GallerySortPreference.NEWEST.mediaSortOrder,
        )
        assertEquals(
            com.goreecloud.gallery.core.MediaSortOrder.OLDEST,
            GallerySortPreference.OLDEST.mediaSortOrder,
        )
    }

    @Test
    fun spaciousDensityKeepsAtLeastTwoColumnsAndIsStrictlyBroaderThanDenseWhenPossible() {
        val compactWidthDp = 240
        assertEquals(2, GalleryViewDensity.SPACIOUS.mediaGridColumns(compactWidthDp))

        val wideWidthDp = 900
        val dense = GalleryViewDensity.DENSE.mediaGridColumns(wideWidthDp)
        val comfortable = GalleryViewDensity.COMFORTABLE.mediaGridColumns(wideWidthDp)
        val spacious = GalleryViewDensity.SPACIOUS.mediaGridColumns(wideWidthDp)
        assertEquals(dense - 1, comfortable)
        assertEquals(dense - 2, spacious)
    }
}
