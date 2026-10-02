package com.goreecloud.gallery

internal object GallerySlideshowPolicy {
    const val DEFAULT_INTERVAL_MS = 5_000L

    fun nextPhotoIndex(
        currentIndex: Int,
        photoEligibility: List<Boolean>,
        loop: Boolean = false,
    ): Int? {
        if (currentIndex !in photoEligibility.indices) return null
        for (index in (currentIndex + 1)..photoEligibility.lastIndex) {
            if (photoEligibility[index]) return index
        }
        if (!loop) return null
        for (index in 0 until currentIndex) {
            if (photoEligibility[index]) return index
        }
        return null
    }
}
