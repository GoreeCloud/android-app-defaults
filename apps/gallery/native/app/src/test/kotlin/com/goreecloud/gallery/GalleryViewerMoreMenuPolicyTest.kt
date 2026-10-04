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
        )

        assertTrue(state.canOpenContainingAlbum)
    }

    @Test
    fun staleItemCannotReuseAnAuthorizedAlbum() {
        val state = GalleryViewerMoreMenuPolicy.state(
            currentContentUri = "content://media/external/images/media/7",
            currentAlbumId = "camera",
            authorizedContentUris = setOf("content://media/external/images/media/8"),
            authorizedAlbumIds = setOf("camera"),
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
            ).canOpenContainingAlbum,
        )
        assertFalse(
            GalleryViewerMoreMenuPolicy.state(
                currentContentUri = "content://media/external/images/media/7",
                currentAlbumId = "",
                authorizedContentUris = setOf("content://media/external/images/media/7"),
                authorizedAlbumIds = setOf(""),
            ).canOpenContainingAlbum,
        )
    }
}
