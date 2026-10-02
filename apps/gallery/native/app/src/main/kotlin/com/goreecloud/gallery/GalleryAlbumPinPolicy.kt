package com.goreecloud.gallery

internal object GalleryAlbumPinPolicy {
    fun orderedIds(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
    ): List<String> {
        val available = availableAlbumIds
            .filter(String::isNotBlank)
            .distinct()
        if (available.isEmpty() || pinnedAlbumIds.isEmpty()) return available

        val pinned = available.filter { it in pinnedAlbumIds }
        val unpinned = available.filterNot { it in pinnedAlbumIds }
        return pinned + unpinned
    }

    fun toggled(
        pinnedAlbumIds: Set<String>,
        albumId: String,
        pinned: Boolean,
    ): Set<String> {
        require(albumId.isNotBlank()) { "album id is required" }
        return pinnedAlbumIds.toMutableSet().apply {
            if (pinned) add(albumId) else remove(albumId)
        }.toSet()
    }
}
