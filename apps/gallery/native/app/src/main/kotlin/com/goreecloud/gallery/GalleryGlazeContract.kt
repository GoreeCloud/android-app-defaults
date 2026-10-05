package com.goreecloud.gallery

/**
 * Gallery's repository-local native mapping of the current consumer-eligible Glaze V1.7 anchor.
 *
 * Glaze V1.7 / 1.7.0 intentionally inherits the accepted V1.6 runtime. Gallery therefore adopts
 * the V1.7 identity and bounded stable scope without consuming retained dev.47 or Section 48
 * Development behavior. These values govern presentation only and grant no permission, media,
 * privacy, security, mutation, distribution, or release authority.
 */
object GalleryGlazeContract {
    const val VERSION = "1.7.0"
    const val PRODUCT_LABEL = "Glaze V1.7"
    const val SOURCE_QUALIFICATION_ANCHOR = "7c4ded83d7a8725165bb6a55dfb175667cc9589e"
    const val STABLE_RUNTIME_ENTRYPOINT = "js/glaze-v1.7.0.mjs"
    const val INHERITED_RUNTIME_ENTRYPOINT = "js/glaze-v1.6.0.mjs"
    const val INHERITED_ACCEPTED_RELEASE_SOURCE = "a7180679ea851389e0f3004515f9a25f420e716d"
    const val ROLLBACK_BASELINE = "1.6.0"
    const val RETAINED_DEVELOPMENT_SOURCE_INCLUDED = false
    const val SECTION_48_INCLUDED = false

    const val PRESENTATION_ONLY = true
    const val PERMISSION_REQUEST_AUTOMATIC = false
    const val AUTHORIZATION_INFERRED = false
    const val CONSEQUENTIAL_EXECUTION_AUTOMATIC = false
    const val DOWNSTREAM_CONSUMER_ACCEPTANCE_AUTOMATIC = false

    enum class MaterialRole {
        CANVAS,
        SOLID,
        RAISED,
        FUNCTIONAL_GLASS,
        CLEAR_GLASS,
        OVERLAY,
    }

    enum class WidthClass {
        COMPACT,
        MEDIUM,
        EXPANDED,
    }

    const val SPACE_MICRO_DP = 2
    const val SPACE_HAIRLINE_DP = 4
    const val SPACE_CONTROL_DP = 8
    const val SPACE_COMPACT_CLUSTER_DP = 12
    const val SPACE_STANDARD_CLUSTER_DP = 16
    const val SPACE_CONTENT_DP = 24
    const val SPACE_SECTION_DP = 32
    const val SPACE_REGION_DP = 48

    const val SHAPE_QUIET_DP = 10
    const val SHAPE_CONTROL_DP = 14
    const val SHAPE_CONTAINER_DP = 18
    const val SHAPE_ROUNDED_DP = 24
    const val SHAPE_OVERLAY_DP = 28
    const val SHAPE_CAPSULE_DP = 999

    const val MOTION_MICRO_MS = 150L
    const val MOTION_STANDARD_MS = 230L
    const val MOTION_CONNECTED_MS = 340L
    const val MOTION_SPATIAL_MS = 460L
    const val MOTION_REDUCED_STANDARD_MS = 160L
    const val MOTION_MINIMAL_MS = 0L

    const val OPTICAL_MEMORY_TINT_MAX_FRACTION = 0.08f
    const val OPTICAL_CONTENT_AWARE_FROST_ENABLED = true
    const val OPTICAL_SEMANTIC_BLUR_PROTECTION_ENABLED = true
    const val OPTICAL_REDUCED_TRANSPARENCY_FALLBACK_REQUIRED = true
    const val OPTICAL_INCREASED_CONTRAST_FALLBACK_REQUIRED = true
    const val OPTICAL_ENVIRONMENT_TINT_MAY_OVERRIDE_SEMANTIC_STATE = false

    const val GENERAL_TARGET_DP = 48
    const val MAX_RENDERED_MEDIA_ROWS = 100
    const val MIN_GRID_TILE_DP = 92
    const val MIN_ALBUM_TILE_DP = 148
    const val MAX_FEATURED_VIDEO_WIDTH_DP = 760
    const val VIDEO_FEATURED_MIN_WIDTH_DP = 720
    const val CONTENT_MAX_WIDTH_DP = 1440
    const val SETTINGS_MAX_WIDTH_DP = 760

    const val NAVIGATION_HEIGHT_DP = 56
    const val NAVIGATION_RADIUS_DP = 18
    const val NAVIGATION_SIDE_MARGIN_DP = 10
    const val NAVIGATION_BOTTOM_MARGIN_DP = 8
    const val NAVIGATION_ELEVATION_DP = 2
    const val NAVIGATION_RESERVED_SPACE_DP = 72
    const val CONTENT_BOTTOM_INSET_DP = 18
    const val NAVIGATION_ICON_DP = 22
    const val NAVIGATION_LABEL_SP = 10.5f
    const val NAVIGATION_ITEM_RADIUS_DP = 14
    const val NAVIGATION_ITEM_HORIZONTAL_PADDING_DP = 0
    const val NAVIGATION_ITEM_VERTICAL_PADDING_DP = 0
    const val NAVIGATION_SELECTED_HORIZONTAL_INSET_DP = 14
    const val NAVIGATION_SELECTED_VERTICAL_INSET_DP = 7
    const val NAVIGATION_RAIL_WIDTH_DP = 64
    const val NAVIGATION_RAIL_SIDE_MARGIN_DP = 12
    const val NAVIGATION_RAIL_ITEM_HEIGHT_DP = 52

    fun widthClass(widthDp: Int): WidthClass = when {
        widthDp >= 840 -> WidthClass.EXPANDED
        widthDp >= 600 -> WidthClass.MEDIUM
        else -> WidthClass.COMPACT
    }

    fun usesNavigationRail(widthDp: Int): Boolean =
        widthClass(widthDp) == WidthClass.EXPANDED

    fun navigationRailLaneDp(widthDp: Int): Int =
        if (usesNavigationRail(widthDp)) {
            NAVIGATION_RAIL_WIDTH_DP + (NAVIGATION_RAIL_SIDE_MARGIN_DP * 2)
        } else {
            0
        }

    fun horizontalGutterDp(widthDp: Int): Int = when (widthClass(widthDp)) {
        WidthClass.COMPACT -> if (widthDp < 360) 10 else 12
        WidthClass.MEDIUM -> SPACE_CONTENT_DP
        WidthClass.EXPANDED -> SPACE_SECTION_DP
    }

    fun headerTitleSp(widthDp: Int): Float = when (widthClass(widthDp)) {
        WidthClass.COMPACT -> if (widthDp < 360) 25f else 28f
        WidthClass.MEDIUM -> 30f
        WidthClass.EXPANDED -> 32f
    }

    fun contentTopPaddingDp(widthDp: Int): Int = when (widthClass(widthDp)) {
        WidthClass.COMPACT -> 8
        WidthClass.MEDIUM -> 12
        WidthClass.EXPANDED -> 16
    }

    fun mediaGapDp(widthDp: Int): Int = when (widthClass(widthDp)) {
        WidthClass.COMPACT -> 3
        WidthClass.MEDIUM -> 5
        WidthClass.EXPANDED -> 6
    }

    fun gridColumns(widthDp: Int): Int = when {
        widthDp >= 1280 -> 8
        widthDp >= 1000 -> 7
        widthDp >= 840 -> 6
        widthDp >= 600 -> 5
        widthDp >= 430 -> 4
        else -> 3
    }

    fun albumGridColumns(widthDp: Int): Int = when {
        widthDp >= 1200 -> 5
        widthDp >= 840 -> 4
        widthDp >= 600 -> 3
        else -> 2
    }

    fun videoGridColumns(widthDp: Int): Int = when {
        widthDp >= 1200 -> 4
        widthDp >= 720 -> 3
        widthDp >= 360 -> 2
        else -> 1
    }

    fun videoUsesFeaturedCard(widthDp: Int): Boolean =
        widthDp >= VIDEO_FEATURED_MIN_WIDTH_DP

    fun trashGridColumns(widthDp: Int): Int = when {
        widthDp >= 1200 -> 6
        widthDp >= 840 -> 5
        widthDp >= 600 -> 4
        else -> 3
    }
}
