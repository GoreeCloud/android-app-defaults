package com.goreecloud.gallery

internal object GallerySlideshowPolicy {
    const val DEFAULT_INTERVAL_MS = 5_000L

    fun nextPhotoIndex(
        currentIndex: Int,
        photoEligibility: List<Boolean>,
        repeat: Boolean = false,
    ): Int? {
        if (currentIndex !in photoEligibility.indices) return null
        for (index in (currentIndex + 1)..photoEligibility.lastIndex) {
            if (photoEligibility[index]) return index
        }
        if (!repeat) return null
        val firstPhotoIndex = photoEligibility.indexOfFirst { it }
        return firstPhotoIndex.takeIf { it >= 0 && it != currentIndex }
    }
}
