package com.goreecloud.gallery

enum class GalleryNavigationIconPlacement {
    NONE,
    CENTERED,
    ABOVE_LABEL,
}

object GalleryNavigationPresentationPolicy {
    fun iconPlacement(mode: GalleryNavigationDisplayMode): GalleryNavigationIconPlacement =
        when {
            !mode.showIcon -> GalleryNavigationIconPlacement.NONE
            mode.showLabel -> GalleryNavigationIconPlacement.ABOVE_LABEL
            else -> GalleryNavigationIconPlacement.CENTERED
        }

    fun selectedHorizontalInsetDp(mode: GalleryNavigationDisplayMode): Int =
        if (mode == GalleryNavigationDisplayMode.ICONS_ONLY) {
            GalleryGlazeContract.NAVIGATION_ICON_ONLY_SELECTED_HORIZONTAL_INSET_DP
        } else {
            GalleryGlazeContract.NAVIGATION_LABELED_SELECTED_HORIZONTAL_INSET_DP
        }

    fun selectedVerticalInsetDp(mode: GalleryNavigationDisplayMode): Int =
        if (mode == GalleryNavigationDisplayMode.ICONS_ONLY) {
            GalleryGlazeContract.NAVIGATION_ICON_ONLY_SELECTED_VERTICAL_INSET_DP
        } else {
            GalleryGlazeContract.NAVIGATION_LABELED_SELECTED_VERTICAL_INSET_DP
        }
}
