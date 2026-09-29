package com.goreecloud.gallery

/**
 * Pure routing contract for full-screen Gallery viewer bitmap loading.
 *
 * Image media with a valid measured viewport use the orientation-aware ImageDecoder path first.
 * Videos, unknown media, and pre-layout/invalid viewport states retain thumbnail loading. This
 * policy grants no content authority and does not change grid or album thumbnail behavior.
 */
object GalleryViewerLoadPolicy {
    fun primaryPath(
        mimeType: String,
        viewportWidth: Int,
        viewportHeight: Int,
    ): GalleryViewerLoadPath = if (
        GalleryViewerDecodePolicy.usesImageDecoder(mimeType) &&
        viewportWidth > 0 &&
        viewportHeight > 0
    ) {
        GalleryViewerLoadPath.ORIENTATION_AWARE_IMAGE
    } else {
        GalleryViewerLoadPath.THUMBNAIL
    }
}

enum class GalleryViewerLoadPath {
    ORIENTATION_AWARE_IMAGE,
    THUMBNAIL,
}
