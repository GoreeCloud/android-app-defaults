package com.goreecloud.gallery

import kotlin.math.abs
import kotlin.math.roundToInt

data class GalleryViewerTranslation(
    val x: Float,
    val y: Float,
)

object GalleryViewerZoomPolicy {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 4f
    const val ACCESSIBLE_PRESET_SCALE = 2f
    private const val ZOOM_EPSILON = 0.01f

    fun scaleAfterGesture(currentScale: Float, scaleFactor: Float): Float {
        val current = currentScale.takeIf { it.isFinite() } ?: MIN_SCALE
        if (!scaleFactor.isFinite() || scaleFactor <= 0f) return current.coerceIn(MIN_SCALE, MAX_SCALE)
        return (current * scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun isZoomed(scale: Float): Boolean =
        scale.isFinite() && scale > MIN_SCALE + ZOOM_EPSILON

    fun boundedTranslation(
        viewportWidth: Int,
        viewportHeight: Int,
        scale: Float,
        proposedX: Float,
        proposedY: Float,
    ): GalleryViewerTranslation {
        if (viewportWidth <= 0 || viewportHeight <= 0 || !isZoomed(scale)) {
            return GalleryViewerTranslation(0f, 0f)
        }

        val safeScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        val maxX = viewportWidth * (safeScale - 1f) / 2f
        val maxY = viewportHeight * (safeScale - 1f) / 2f
        val x = proposedX.takeIf { it.isFinite() }?.coerceIn(-maxX, maxX) ?: 0f
        val y = proposedY.takeIf { it.isFinite() }?.coerceIn(-maxY, maxY) ?: 0f
        return GalleryViewerTranslation(x, y)
    }

    fun displayPercent(scale: Float): Int {
        val safeScale = scale.takeIf { it.isFinite() }?.coerceIn(MIN_SCALE, MAX_SCALE) ?: MIN_SCALE
        return (safeScale * 100f).roundToInt()
    }

    fun allowsNavigationSwipe(scale: Float, hadMultiplePointers: Boolean): Boolean =
        !hadMultiplePointers && !isZoomed(scale)

    fun isPreset(scale: Float): Boolean =
        scale.isFinite() && abs(scale - ACCESSIBLE_PRESET_SCALE) <= ZOOM_EPSILON
}
