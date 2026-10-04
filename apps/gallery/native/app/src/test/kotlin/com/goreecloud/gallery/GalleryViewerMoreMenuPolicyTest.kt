package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryViewerMoreMenuPolicyTest {
    @Test
    fun authorizedItemAndAlbumCanOpenContainingAlbum() {
        val state = GalleryViewerMoreMenuPolicy.state(
            currentContentUri = "content://media/external/images/media/7",
            currentAlbumId = "camera",
            authorizedContentUris = setOf("content://media/external/images/media/7"),
            authorizedAlbumIds = setOf("camera"),
        favoriteContentUris = emptySet(),
        )

        assertTrue(state.canOpenContainingAlbum)
    }

    @Test
    fun favoriteItemCanOpenFavoritesOnlyWhileStillAuthorized() {
        val uri = "content://media/external/images/media/7"

        assertTrue(
            GalleryViewerMoreMenuPolicy.state(
                currentContentUri = uri,
                currentAlbumId = "camera",
                authorizedContentUris = setOf(uri),
                authorizedAlbumIds = setOf("camera"),
                favoriteContentUris = setOf(uri),
            ).canOpenFavorites,
        )
        assertFalse(
            GalleryViewerMoreMenuPolicy.state(
                currentContentUri = uri,
                currentAlbumId = "camera",
                authorizedContentUris = emptySet(),
                authorizedAlbumIds = setOf("camera"),
                favoriteContentUris = setOf(uri),
            ).canOpenFavorites,
        )
    }

    @Test
    fun staleItemCannotReuseAnAuthorizedAlbum() {
        val state = GalleryViewerMoreMenuPolicy.state(
            currentContentUri = "content://media/external/images/media/7",
            currentAlbumId = "camera",
            authorizedContentUris = setOf("content://media/external/images/media/8"),
            authorizedAlbumIds = setOf("camera"),
        favoriteContentUris = emptySet(),
        )

        assertFalse(state.canOpenContainingAlbum)
    }

    @Test
    fun staleOrBlankAlbumCannotBeOpened() {
        assertFalse(
            GalleryViewerMoreMenuPolicy.state(
                currentContentUri = "content://media/external/images/media/7",
                currentAlbumId = "old-camera",
                authorizedContentUris = setOf("content://media/external/images/media/7"),
                authorizedAlbumIds = setOf("camera"),
            favoriteContentUris = emptySet(),
            ).canOpenContainingAlbum,
        )
        assertFalse(
            GalleryViewerMoreMenuPolicy.state(
                currentContentUri = "content://media/external/images/media/7",
                currentAlbumId = "",
                authorizedContentUris = setOf("content://media/external/images/media/7"),
                authorizedAlbumIds = setOf(""),
            favoriteContentUris = emptySet(),
            ).canOpenContainingAlbum,
        )
    }
}
