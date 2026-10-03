package com.goreecloud.gallery

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

/**
 * Shared visual treatment for Gallery's five primary bottom-navigation destinations.
 *
 * Navigation semantics and destination authority stay owned by the Activities. This object only
 * applies Glaze presentation so Gallery and Trash render the same geometry from the first frame.
 */
object GalleryNavigationStyling {
    fun apply(
        activity: Activity,
        item: TextView,
        label: String,
        iconRes: Int,
        mode: GalleryNavigationDisplayMode,
        selected: Boolean,
        foreground: Int,
    ) {
        val placement = GalleryNavigationPresentationPolicy.iconPlacement(mode)
        val drawable = if (placement == GalleryNavigationIconPlacement.NONE) {
            null
        } else {
            activity.getDrawable(iconRes)?.mutate()?.apply {
                val iconPx = dp(activity, GalleryGlazeContract.NAVIGATION_ICON_DP)
                setBounds(0, 0, iconPx, iconPx)
            }
        }

        item.text = if (mode.showLabel) label else ""
        item.gravity = Gravity.CENTER
        item.textAlignment = View.TEXT_ALIGNMENT_CENTER
        item.includeFontPadding = false
        item.minHeight = dp(activity, GalleryGlazeContract.GENERAL_TARGET_DP)
        item.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            GalleryGlazeContract.NAVIGATION_LABEL_SP,
        )
        item.setTextColor(foreground)
        item.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        item.setPadding(
            dp(activity, GalleryGlazeContract.NAVIGATION_ITEM_HORIZONTAL_PADDING_DP),
            dp(activity, GalleryGlazeContract.NAVIGATION_ITEM_VERTICAL_PADDING_DP),
            dp(activity, GalleryGlazeContract.NAVIGATION_ITEM_HORIZONTAL_PADDING_DP),
            dp(activity, GalleryGlazeContract.NAVIGATION_ITEM_VERTICAL_PADDING_DP),
        )

        when (placement) {
            GalleryNavigationIconPlacement.NONE ->
                item.setCompoundDrawables(null, null, null, null)
            GalleryNavigationIconPlacement.CENTERED ->
                item.setCompoundDrawables(drawable, null, null, null)
            GalleryNavigationIconPlacement.ABOVE_LABEL ->
                item.setCompoundDrawables(null, drawable, null, null)
        }
        item.compoundDrawableTintList = ColorStateList.valueOf(foreground)
        item.compoundDrawablePadding =
            if (placement == GalleryNavigationIconPlacement.ABOVE_LABEL) dp(activity, 2) else 0

        item.background = if (selected) {
            InsetDrawable(
                GalleryGlazeSurfaces.drawable(
                    activity,
                    GalleryGlazeSurfaces.Role.CONTROL,
                    GalleryGlazeContract.NAVIGATION_ITEM_RADIUS_DP,
                ),
                dp(activity, GalleryNavigationPresentationPolicy.selectedHorizontalInsetDp(mode)),
                dp(activity, GalleryNavigationPresentationPolicy.selectedVerticalInsetDp(mode)),
                dp(activity, GalleryNavigationPresentationPolicy.selectedHorizontalInsetDp(mode)),
                dp(activity, GalleryNavigationPresentationPolicy.selectedVerticalInsetDp(mode)),
            )
        } else {
            ColorDrawable(Color.TRANSPARENT)
        }

        item.foreground = RippleDrawable(
            ColorStateList.valueOf(
                Color.argb(
                    32,
                    Color.red(foreground),
                    Color.green(foreground),
                    Color.blue(foreground),
                ),
            ),
            null,
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(
                    activity,
                    GalleryGlazeContract.NAVIGATION_ITEM_RADIUS_DP,
                ).toFloat()
                setColor(Color.WHITE)
            },
        )
        item.tooltipText = label
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
