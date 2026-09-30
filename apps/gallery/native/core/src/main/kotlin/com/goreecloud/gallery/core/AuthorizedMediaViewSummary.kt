package com.goreecloud.gallery.core

data class AuthorizedMediaViewSummary(
    val authorizedCount: Int,
    val presentedCount: Int,
    val albumName: String?,
    val mediaTypeFilter: MediaTypeFilter,
    val sortOrder: MediaSortOrder,
) {
    val hasNonDefaultControls: Boolean
        get() = albumName != null || mediaTypeFilter != MediaTypeFilter.ALL || sortOrder != MediaSortOrder.NEWEST
}

fun summarizeAuthorizedMediaView(
    authorizedItems: List<MediaItem>,
    mediaTypeFilter: MediaTypeFilter,
    sortOrder: MediaSortOrder,
    albumId: String?,
): AuthorizedMediaViewSummary {
    val albumName = mediaAlbumOptions(authorizedItems).firstOrNull { it.albumId == albumId }?.albumName
    val presentedCount = mediaTypeFilter.filter(filterAuthorizedAlbum(authorizedItems, albumId)).size
    return AuthorizedMediaViewSummary(
        authorizedCount = authorizedItems.size,
        presentedCount = presentedCount,
        albumName = albumName,
        mediaTypeFilter = mediaTypeFilter,
        sortOrder = sortOrder,
    )
}
