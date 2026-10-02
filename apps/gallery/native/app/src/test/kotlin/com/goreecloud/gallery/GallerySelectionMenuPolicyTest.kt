package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GallerySelectionMenuPolicyTest {
    @Test
    fun partialSelectionOffersSelectAllAndSingleItemDetails() {
        val state = GallerySelectionMenuPolicy.state(
            visibleContentUris = listOf("content://media/1", "content://media/2"),
            selectedContentUris = setOf("content://media/1"),
        )

        assertEquals("Select all visible", state.primaryActionLabel)
        assertTrue(state.showDetails)
    }

    @Test
    fun completeVisibleSelectionOffersClearAndHidesMultiItemDetails() {
        val state = GallerySelectionMenuPolicy.state(
            visibleContentUris = listOf("content://media/1", "content://media/2"),
            selectedContentUris = setOf("content://media/1", "content://media/2"),
        )

        assertEquals("Clear selection", state.primaryActionLabel)
        assertFalse(state.showDetails)
    }

    @Test
    fun staleOrEmptySelectionCannotBecomeClearAllAuthority() {
        val state = GallerySelectionMenuPolicy.state(
            visibleContentUris = listOf("content://media/1"),
            selectedContentUris = setOf("content://media/stale"),
        )
        val empty = GallerySelectionMenuPolicy.state(
            visibleContentUris = emptyList(),
            selectedContentUris = emptySet(),
        )

        assertEquals("Select all visible", state.primaryActionLabel)
        assertTrue(state.showDetails)
        assertEquals("Select all visible", empty.primaryActionLabel)
        assertFalse(empty.showDetails)
    }
}
