package com.goreecloud.gallery

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

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
        val stateChanged = item.isSelected != selected
        val placement = GalleryNavigationPresentationPolicy.iconPlacement(mode)
        val drawable = activity.getDrawable(iconRes)?.mutate()?.apply {
            val iconPx = dp(activity, GalleryGlazeContract.NAVIGATION_ICON_DP)
            setBounds(0, 0, iconPx, iconPx)
        }

        item.text = ""
        item.gravity = Gravity.CENTER
        item.textAlignment = View.TEXT_ALIGNMENT_CENTER
        item.includeFontPadding = false
        item.minimumHeight = dp(activity, GalleryGlazeContract.GENERAL_TARGET_DP)
        item.setPadding(0, 0, 0, 0)

        when (placement) {
            GalleryNavigationIconPlacement.NONE -> item.setCompoundDrawables(null, null, null, null)
            GalleryNavigationIconPlacement.CENTERED -> item.setCompoundDrawables(drawable, null, null, null)
            GalleryNavigationIconPlacement.ABOVE_LABEL -> item.setCompoundDrawables(null, drawable, null, null)
        }
        item.compoundDrawableTintList = ColorStateList.valueOf(foreground)
        item.compoundDrawablePadding = 0

        item.background = if (selected) {
            InsetDrawable(
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(activity, GalleryGlazeContract.NAVIGATION_ITEM_RADIUS_DP).toFloat()
                    setColor(Color.argb(22, Color.red(foreground), Color.green(foreground), Color.blue(foreground)))
                    setStroke(
                        dp(activity, 1).coerceAtLeast(1),
                        Color.argb(38, Color.red(foreground), Color.green(foreground), Color.blue(foreground)),
                    )
                },
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
                Color.argb(28, Color.red(foreground), Color.green(foreground), Color.blue(foreground)),
            ),
            null,
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(activity, GalleryGlazeContract.NAVIGATION_ITEM_RADIUS_DP).toFloat()
                setColor(Color.WHITE)
            },
        )

        val targetAlpha = if (selected) 1f else 0.72f
        item.animate().cancel()
        if (stateChanged) {
            item.alpha = if (selected) 0.72f else 1f
            item.scaleX = if (selected) 0.94f else 1f
            item.scaleY = if (selected) 0.94f else 1f
            item.animate()
                .alpha(targetAlpha)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(GalleryGlazeContract.MOTION_MICRO_MS)
                .start()
        } else {
            item.alpha = targetAlpha
            item.scaleX = 1f
            item.scaleY = 1f
        }

        item.tooltipText = label
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
