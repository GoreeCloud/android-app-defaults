package com.goreecloud.gallery

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView

/**
 * Gallery 0.9 presentation primitives.
 *
 * The activity owns navigation and media authority. This object owns repeatable visual decisions so
 * browsing, search, settings, Trash, and viewer chrome share one Glaze-native presentation model.
 */
object GalleryDesignSystem {
    data class Metrics(
        val widthClass: GalleryGlazeContract.WidthClass,
        val horizontalGutterDp: Int,
        val topPaddingDp: Int,
        val mediaGapDp: Int,
        val titleSp: Float,
        val commandRadiusDp: Int,
        val sectionSpacingDp: Int,
        val navigationHeightDp: Int,
        val navigationSideMarginDp: Int,
        val navigationBottomMarginDp: Int,
        val navigationRadiusDp: Int,
    )

    fun metrics(context: Context): Metrics {
        val widthDp = context.resources.configuration.screenWidthDp
        val widthClass = GalleryGlazeContract.widthClass(widthDp)
        return Metrics(
            widthClass = widthClass,
            horizontalGutterDp = GalleryGlazeContract.horizontalGutterDp(widthDp),
            topPaddingDp = GalleryGlazeContract.contentTopPaddingDp(widthDp),
            mediaGapDp = GalleryGlazeContract.mediaGapDp(widthDp),
            titleSp = GalleryGlazeContract.headerTitleSp(widthDp),
            commandRadiusDp = when (widthClass) {
                GalleryGlazeContract.WidthClass.COMPACT -> 15
                GalleryGlazeContract.WidthClass.MEDIUM -> 17
                GalleryGlazeContract.WidthClass.EXPANDED -> 18
            },
            sectionSpacingDp = when (widthClass) {
                GalleryGlazeContract.WidthClass.COMPACT -> 20
                GalleryGlazeContract.WidthClass.MEDIUM -> 26
                GalleryGlazeContract.WidthClass.EXPANDED -> 30
            },
            navigationHeightDp = GalleryGlazeContract.NAVIGATION_HEIGHT_DP,
            navigationSideMarginDp = when (widthClass) {
                GalleryGlazeContract.WidthClass.COMPACT -> GalleryGlazeContract.NAVIGATION_SIDE_MARGIN_DP
                GalleryGlazeContract.WidthClass.MEDIUM -> 18
                GalleryGlazeContract.WidthClass.EXPANDED -> 28
            },
            navigationBottomMarginDp = GalleryGlazeContract.NAVIGATION_BOTTOM_MARGIN_DP,
            navigationRadiusDp = GalleryGlazeContract.NAVIGATION_RADIUS_DP,
        )
    }

    fun applyTitle(text: TextView, primary: Int) {
        val m = metrics(text.context)
        text.setTextColor(primary)
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, m.titleSp)
        text.setTypeface(text.typeface, Typeface.BOLD)
        text.includeFontPadding = false
        text.letterSpacing = -0.018f
        text.maxLines = 1
    }

    fun applySubtitle(text: TextView, secondary: Int) {
        text.setTextColor(secondary)
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        text.includeFontPadding = false
        text.maxLines = 1
        text.letterSpacing = 0.01f
    }

    fun applySectionLabel(text: TextView, primary: Int) {
        text.setTextColor(primary)
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        text.setTypeface(text.typeface, Typeface.BOLD)
        text.includeFontPadding = false
        text.letterSpacing = -0.006f
    }

    fun applySupportingText(text: TextView, secondary: Int) {
        text.setTextColor(secondary)
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        text.includeFontPadding = false
        text.setLineSpacing(0f, 1.08f)
    }

    fun iconButtonBackground(
        context: Context,
        foreground: Int,
        selected: Boolean = false,
        destructive: Boolean = false,
    ): RippleDrawable {
        val radius = dp(context, metrics(context).commandRadiusDp).toFloat()
        val baseAlpha = when {
            destructive -> 16
            selected -> 28
            else -> 10
        }
        val strokeAlpha = when {
            destructive -> 38
            selected -> 54
            else -> 24
        }
        val base = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(
                Color.argb(
                    baseAlpha,
                    Color.red(foreground),
                    Color.green(foreground),
                    Color.blue(foreground),
                ),
            )
            setStroke(
                dp(context, 1).coerceAtLeast(1),
                Color.argb(
                    strokeAlpha,
                    Color.red(foreground),
                    Color.green(foreground),
                    Color.blue(foreground),
                ),
            )
        }
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            ColorStateList.valueOf(
                Color.argb(
                    if (destructive) 44 else 30,
                    Color.red(foreground),
                    Color.green(foreground),
                    Color.blue(foreground),
                ),
            ),
            base,
            mask,
        )
    }

    fun applyIconButton(
        view: View,
        foreground: Int,
        selected: Boolean = false,
        destructive: Boolean = false,
    ) {
        view.minimumWidth = dp(view.context, GalleryGlazeContract.GENERAL_TARGET_DP)
        view.minimumHeight = dp(view.context, GalleryGlazeContract.GENERAL_TARGET_DP)
        view.background = iconButtonBackground(view.context, foreground, selected, destructive)
        view.elevation = 0f
        view.isFocusable = true
        view.isClickable = true
    }

    fun selectionSurface(context: Context, accent: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, GalleryGlazeContract.SHAPE_CONTROL_DP).toFloat()
            setColor(withAlpha(accent, 0.12f))
            setStroke(dp(context, 1), withAlpha(accent, 0.28f))
        }

    fun emptyStateSurface(context: Context): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, GalleryGlazeContract.SHAPE_CONTAINER_DP).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(dp(context, 1), context.getColor(R.color.gallery_divider))
        }

    fun settingGroupSurface(context: Context): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, GalleryGlazeContract.SHAPE_CONTAINER_DP).toFloat()
            setColor(context.getColor(R.color.gallery_surface))
            setStroke(dp(context, 1), context.getColor(R.color.gallery_divider))
        }

    fun mediaChromeSurface(
        context: Context,
        radiusDp: Int = GalleryGlazeContract.SHAPE_ROUNDED_DP,
        strong: Boolean = false,
    ): GradientDrawable {
        val top = if (strong) Color.rgb(28, 34, 32) else Color.rgb(24, 30, 28)
        val bottom = if (strong) Color.rgb(17, 22, 20) else Color.rgb(14, 19, 17)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(withAlpha(top, 0.97f), withAlpha(bottom, 0.94f)),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setStroke(dp(context, 1), withAlpha(Color.WHITE, if (strong) 0.16f else 0.11f))
        }
    }

    fun mediaIconBackground(
        context: Context,
        foreground: Int = Color.WHITE,
        selected: Boolean = false,
        destructive: Boolean = false,
    ): RippleDrawable = iconButtonBackground(
        context = context,
        foreground = foreground,
        selected = selected,
        destructive = destructive,
    )

    fun navigationForeground(selected: Boolean, accent: Int, secondary: Int): Int =
        if (selected) accent else secondary

    fun gravityForPrimaryText(): Int = Gravity.CENTER_VERTICAL or Gravity.START

    fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (255f * alpha.coerceIn(0f, 1f)).toInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
