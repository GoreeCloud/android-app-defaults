package com.goreecloud.gallery

import kotlin.math.roundToInt

/**
 * Pure sizing and media-routing policy for the bounded native Gallery viewer.
 *
 * The Android decoder remains responsible for interpreting encoded image orientation. This policy
 * only decides whether the image-specific decoder path is eligible and how large a decoded bitmap
 * may become. It grants no MediaStore or content-URI authority.
 */
object GalleryViewerDecodePolicy {
    const val MAX_LONG_EDGE_PX = 2048

    fun usesImageDecoder(mimeType: String): Boolean =
        mimeType.trim().lowercase().startsWith("image/")

    fun boundedDecodeSize(
        sourceWidth: Int,
        sourceHeight: Int,
        viewportWidth: Int,
        viewportHeight: Int,
    ): GalleryViewerDecodeSize {
        require(sourceWidth > 0) { "sourceWidth must be positive" }
        require(sourceHeight > 0) { "sourceHeight must be positive" }
        require(viewportWidth > 0) { "viewportWidth must be positive" }
        require(viewportHeight > 0) { "viewportHeight must be positive" }

        val maxWidth = minOf(viewportWidth, MAX_LONG_EDGE_PX)
        val maxHeight = minOf(viewportHeight, MAX_LONG_EDGE_PX)
        val scale = minOf(
            1.0,
            maxWidth.toDouble() / sourceWidth.toDouble(),
            maxHeight.toDouble() / sourceHeight.toDouble(),
        )

        return GalleryViewerDecodeSize(
            width = maxOf(1, (sourceWidth * scale).roundToInt()),
            height = maxOf(1, (sourceHeight * scale).roundToInt()),
        )
    }
}

data class GalleryViewerDecodeSize(
    val width: Int,
    val height: Int,
)
