package com.goreecloud.gallery

import kotlin.math.roundToInt

internal object GalleryAnimatedThumbnailPolicy {
    const val MAX_TARGET_EDGE_PX = 512

    fun isAnimatedGif(mimeType: String): Boolean =
        mimeType.trim().equals("image/gif", ignoreCase = true)

    fun targetSize(
        sourceWidth: Int,
        sourceHeight: Int,
        requestedEdgePx: Int,
    ): Pair<Int, Int>? {
        if (sourceWidth <= 0 || sourceHeight <= 0 || requestedEdgePx <= 0) return null
        val targetEdge = minOf(requestedEdgePx, MAX_TARGET_EDGE_PX)
        val sourceLongEdge = maxOf(sourceWidth, sourceHeight)
        val scale = minOf(1.0, targetEdge.toDouble() / sourceLongEdge.toDouble())
        return Pair(
            maxOf(1, (sourceWidth * scale).roundToInt()),
            maxOf(1, (sourceHeight * scale).roundToInt()),
        )
    }
}
