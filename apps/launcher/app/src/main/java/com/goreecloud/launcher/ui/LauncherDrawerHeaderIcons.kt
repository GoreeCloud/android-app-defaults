package com.goreecloud.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Compact Launcher-owned line icons for the App Drawer header.
 *
 * Geometry uses one optical grid, rounded caps/joins, and a shared stroke so the three adjacent
 * controls read as one family instead of unrelated mini-illustrations.
 */
@Composable
internal fun LauncherDrawerSortIcon(
    ascending: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val u = size.minDimension
        val stroke = 1.9.dp.toPx()
        val cap = StrokeCap.Round
        val arrowX = u * .27f
        val top = u * .20f
        val bottom = u * .80f
        val tipY = if (ascending) top else bottom
        val shoulderY = if (ascending) u * .35f else u * .65f

        drawLine(color, Offset(arrowX, top), Offset(arrowX, bottom), stroke, cap = cap)
        drawLine(color, Offset(arrowX, tipY), Offset(u * .16f, shoulderY), stroke, cap = cap)
        drawLine(color, Offset(arrowX, tipY), Offset(u * .38f, shoulderY), stroke, cap = cap)

        listOf(
            Triple(.31f, .50f, .82f),
            Triple(.50f, .50f, .73f),
            Triple(.69f, .50f, .64f),
        ).forEach { (y, startX, endX) ->
            drawLine(
                color,
                Offset(u * startX, u * y),
                Offset(u * endX, u * y),
                stroke,
                cap = cap,
            )
        }
    }
}

@Composable
internal fun LauncherDrawerNewFolderIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val u = size.minDimension
        val stroke = 1.9.dp.toPx()
        val outline = Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val folder = Path().apply {
            moveTo(u * .12f, u * .32f)
            lineTo(u * .37f, u * .32f)
            lineTo(u * .46f, u * .23f)
            lineTo(u * .63f, u * .23f)
            lineTo(u * .69f, u * .32f)
            lineTo(u * .88f, u * .32f)
            lineTo(u * .88f, u * .79f)
            lineTo(u * .12f, u * .79f)
            close()
        }
        drawPath(folder, color = color, style = outline)

        val cx = u * .64f
        val cy = u * .55f
        val arm = u * .105f
        drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy - arm), Offset(cx, cy + arm), stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun LauncherDrawerSettingsIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val u = size.minDimension
        val stroke = 1.9.dp.toPx()
        val cap = StrokeCap.Round
        val outline = Stroke(width = stroke)

        listOf(.30f, .50f, .70f).forEach { y ->
            drawLine(
                color = color,
                start = Offset(u * .16f, u * y),
                end = Offset(u * .84f, u * y),
                strokeWidth = stroke,
                cap = cap,
            )
        }
        listOf(
            .36f to .30f,
            .64f to .50f,
            .45f to .70f,
        ).forEach { (x, y) ->
            drawCircle(
                color = color,
                radius = u * .07f,
                center = Offset(u * x, u * y),
                style = outline,
            )
        }
    }
}
