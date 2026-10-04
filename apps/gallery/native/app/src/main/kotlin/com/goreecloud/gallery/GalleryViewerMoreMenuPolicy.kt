package com.goreecloud.gallery

data class GalleryViewerMoreMenuState(
    val canOpenContainingAlbum: Boolean,
    val canOpenFavorites: Boolean,
    val canSetAsPhoto: Boolean,
)

/**
 * Keeps viewer contextual navigation bounded to the currently Android-authorized library.
 *
 * A stale viewer item or stale album identifier cannot manufacture navigation authority: both
 * the item URI and album ID must still be present in the current authorized snapshot.
 */
object GalleryViewerMoreMenuPolicy {
    fun state(
        currentContentUri: String,
        currentAlbumId: String?,
        currentMimeType: String,
        authorizedContentUris: Set<String>,
        authorizedAlbumIds: Set<String>,
        favoriteContentUris: Set<String>,
    ): GalleryViewerMoreMenuState {
        val contentAuthorized =
            currentContentUri.isNotBlank() && currentContentUri in authorizedContentUris
        val albumAuthorized =
            currentAlbumId?.takeIf { it.isNotBlank() }?.let(authorizedAlbumIds::contains) == true
        return GalleryViewerMoreMenuState(
            canOpenContainingAlbum = contentAuthorized && albumAuthorized,
            canOpenFavorites = contentAuthorized && currentContentUri in favoriteContentUris,
            canSetAsPhoto = contentAuthorized && currentMimeType.startsWith("image/"),
        )
    }
}
