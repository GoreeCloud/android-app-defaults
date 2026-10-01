package com.goreecloud.gallery

enum class GalleryAlbumQuickAccessKind {
    FAVORITES,
    CAMERA,
    SCREENSHOTS,
    DOWNLOADS,
    SCREEN_RECORDINGS,
}

object GalleryAlbumQuickAccessPolicy {
    fun kind(albumName: String, isFavorites: Boolean): GalleryAlbumQuickAccessKind? {
        if (isFavorites) return GalleryAlbumQuickAccessKind.FAVORITES

        val normalized = albumName.trim().lowercase()
        return when {
            normalized == "camera" || normalized.endsWith("/camera") ->
                GalleryAlbumQuickAccessKind.CAMERA
            "screenshot" in normalized ->
                GalleryAlbumQuickAccessKind.SCREENSHOTS
            "download" in normalized ->
                GalleryAlbumQuickAccessKind.DOWNLOADS
            "screen recording" in normalized ->
                GalleryAlbumQuickAccessKind.SCREEN_RECORDINGS
            else -> null
        }
    }

    fun priority(albumName: String, isFavorites: Boolean): Int? = when (
        kind(albumName, isFavorites)
    ) {
        GalleryAlbumQuickAccessKind.FAVORITES -> 0
        GalleryAlbumQuickAccessKind.CAMERA -> 1
        GalleryAlbumQuickAccessKind.SCREENSHOTS -> 2
        GalleryAlbumQuickAccessKind.DOWNLOADS -> 3
        GalleryAlbumQuickAccessKind.SCREEN_RECORDINGS -> 4
        null -> null
    }
}
