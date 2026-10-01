package com.goreecloud.gallery

object GalleryAlbumQuickAccessPolicy {
    fun priority(albumName: String, isFavorites: Boolean): Int? {
        if (isFavorites) return 0

        val normalized = albumName.trim().lowercase()
        return when {
            normalized == "camera" || normalized.endsWith("/camera") -> 1
            "screenshot" in normalized -> 2
            "download" in normalized -> 3
            "screen recording" in normalized -> 4
            else -> null
        }
    }
}
