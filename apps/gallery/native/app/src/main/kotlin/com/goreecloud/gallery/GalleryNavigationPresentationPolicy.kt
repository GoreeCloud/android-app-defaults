package com.goreecloud.gallery

enum class GalleryNavigationIconPlacement {
    NONE,
    CENTERED,
    ABOVE_LABEL,
}

object GalleryNavigationPresentationPolicy {
    fun iconPlacement(
        @Suppress("UNUSED_PARAMETER") mode: GalleryNavigationDisplayMode,
    ): GalleryNavigationIconPlacement = GalleryNavigationIconPlacement.CENTERED

    fun showsVisibleLabel(
        @Suppress("UNUSED_PARAMETER") mode: GalleryNavigationDisplayMode,
    ): Boolean = false

    fun selectedHorizontalInsetDp(
        @Suppress("UNUSED_PARAMETER") mode: GalleryNavigationDisplayMode,
    ): Int = GalleryGlazeContract.NAVIGATION_SELECTED_HORIZONTAL_INSET_DP

    fun selectedVerticalInsetDp(
        @Suppress("UNUSED_PARAMETER") mode: GalleryNavigationDisplayMode,
    ): Int = GalleryGlazeContract.NAVIGATION_SELECTED_VERTICAL_INSET_DP
}
