package com.goreecloud.gallery

enum class GalleryMediaAccessScope {
    DENIED,
    LEGACY_FULL,
    SELECTED,
    IMAGES,
    VIDEOS,
    IMAGES_AND_VIDEOS,
}

data class GalleryMediaPermissionSnapshot(
    val apiLevel: Int,
    val readExternalStorage: Boolean = false,
    val readMediaImages: Boolean = false,
    val readMediaVideo: Boolean = false,
    val readMediaVisualUserSelected: Boolean = false,
)

object GalleryMediaAccessPolicy {
    fun resolve(snapshot: GalleryMediaPermissionSnapshot): GalleryMediaAccessScope = when {
        snapshot.apiLevel >= 33 && snapshot.readMediaImages && snapshot.readMediaVideo ->
            GalleryMediaAccessScope.IMAGES_AND_VIDEOS
        snapshot.apiLevel >= 33 && snapshot.readMediaImages -> GalleryMediaAccessScope.IMAGES
        snapshot.apiLevel >= 33 && snapshot.readMediaVideo -> GalleryMediaAccessScope.VIDEOS
        snapshot.apiLevel >= 34 && snapshot.readMediaVisualUserSelected -> GalleryMediaAccessScope.SELECTED
        snapshot.apiLevel <= 32 && snapshot.readExternalStorage -> GalleryMediaAccessScope.LEGACY_FULL
        else -> GalleryMediaAccessScope.DENIED
    }

    fun canRead(scope: GalleryMediaAccessScope): Boolean = scope != GalleryMediaAccessScope.DENIED

    /**
     * Reports any authority that exposes less than the complete image-and-video library available
     * to the current Android permission model.
     *
     * Android 14+ user-selected access is partial by item selection. Android 13+ image-only and
     * video-only grants are also partial for Gallery as a mixed-media application and must retain
     * the visible Change access affordance rather than being presented as complete library access.
     */
    fun isPartial(scope: GalleryMediaAccessScope): Boolean = when (scope) {
        GalleryMediaAccessScope.SELECTED,
        GalleryMediaAccessScope.IMAGES,
        GalleryMediaAccessScope.VIDEOS,
        -> true
        GalleryMediaAccessScope.DENIED,
        GalleryMediaAccessScope.LEGACY_FULL,
        GalleryMediaAccessScope.IMAGES_AND_VIDEOS,
        -> false
    }
}
