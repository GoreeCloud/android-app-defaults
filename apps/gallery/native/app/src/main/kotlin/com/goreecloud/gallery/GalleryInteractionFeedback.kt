package com.goreecloud.gallery

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View

/**
 * Shared bounded press feedback for first-party Gallery controls.
 *
 * This is presentation-only. It does not change action authority, focus order, click targets,
 * MediaStore scope, or Android confirmation behavior.
 */
object GalleryInteractionFeedback {
    fun applyBoundedRipple(
        view: View,
        color: Int,
        radiusDp: Int,
    ) {
        val density = view.resources.displayMetrics.density
        val radiusPx = radiusDp * density
        val rippleColor = Color.argb(
            40,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
        view.foreground = RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            null,
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusPx
                setColor(Color.WHITE)
            },
        )
    }
}
