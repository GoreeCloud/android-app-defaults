package com.goreecloud.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherSearchCategory

internal enum class LauncherSystemGlyphSymbol {
    PIN,
    CHECK,
    CLEAR,
    OVERFLOW,
    BATTERY,
    CHARGING,
}

@Composable
internal fun LauncherSystemGlyph(
    symbol: LauncherSystemGlyphSymbol,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension
        val stroke = (u * 0.09f).coerceAtLeast(1.4.dp.toPx())
        val cap = StrokeCap.Round
        when (symbol) {
            LauncherSystemGlyphSymbol.PIN -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.17f,
                    center = Offset(u * 0.50f, u * 0.30f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    Offset(u * 0.50f, u * 0.47f),
                    Offset(u * 0.50f, u * 0.84f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    Offset(u * 0.33f, u * 0.49f),
                    Offset(u * 0.67f, u * 0.49f),
                    stroke,
                    cap = cap,
                )
            }

            LauncherSystemGlyphSymbol.CHECK -> {
                drawLine(
                    tint,
                    Offset(u * 0.18f, u * 0.52f),
                    Offset(u * 0.42f, u * 0.74f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    Offset(u * 0.42f, u * 0.74f),
                    Offset(u * 0.83f, u * 0.28f),
                    stroke,
                    cap = cap,
                )
            }

            LauncherSystemGlyphSymbol.CLEAR -> {
                drawLine(
                    tint,
                    Offset(u * 0.24f, u * 0.24f),
                    Offset(u * 0.76f, u * 0.76f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    Offset(u * 0.76f, u * 0.24f),
                    Offset(u * 0.24f, u * 0.76f),
                    stroke,
                    cap = cap,
                )
            }

            LauncherSystemGlyphSymbol.OVERFLOW -> {
                listOf(0.26f, 0.50f, 0.74f).forEach { y ->
                    drawCircle(
                        color = tint,
                        radius = u * 0.065f,
                        center = Offset(u * 0.50f, u * y),
                    )
                }
            }

            LauncherSystemGlyphSymbol.BATTERY,
            LauncherSystemGlyphSymbol.CHARGING,
            -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.12f, u * 0.28f),
                    size = Size(u * 0.68f, u * 0.44f),
                    cornerRadius = CornerRadius(u * 0.08f),
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.82f, u * 0.39f),
                    size = Size(u * 0.08f, u * 0.22f),
                    cornerRadius = CornerRadius(u * 0.03f),
                )
                if (symbol == LauncherSystemGlyphSymbol.CHARGING) {
                    val bolt = Path().apply {
                        moveTo(u * 0.53f, u * 0.20f)
                        lineTo(u * 0.36f, u * 0.52f)
                        lineTo(u * 0.49f, u * 0.52f)
                        lineTo(u * 0.40f, u * 0.82f)
                        lineTo(u * 0.66f, u * 0.43f)
                        lineTo(u * 0.52f, u * 0.43f)
                        close()
                    }
                    drawPath(bolt, color = tint)
                } else {
                    drawRoundRect(
                        color = tint.copy(alpha = 0.52f),
                        topLeft = Offset(u * 0.19f, u * 0.35f),
                        size = Size(u * 0.40f, u * 0.30f),
                        cornerRadius = CornerRadius(u * 0.05f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun LauncherSearchCategoryVectorGlyph(
    category: LauncherSearchCategory,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val u = size.minDimension
        val stroke = (u * 0.085f).coerceAtLeast(1.4.dp.toPx())
        val cap = StrokeCap.Round
        val round = CornerRadius(u * 0.10f)
        when (category) {
            LauncherSearchCategory.APPLICATION -> {
                listOf(
                    0.16f to 0.16f,
                    0.55f to 0.16f,
                    0.16f to 0.55f,
                    0.55f to 0.55f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(u * x, u * y),
                        size = Size(u * 0.28f, u * 0.28f),
                        cornerRadius = CornerRadius(u * 0.06f),
                        style = Stroke(width = stroke),
                    )
                }
            }

            LauncherSearchCategory.SHORTCUT,
            LauncherSearchCategory.ACTION,
            -> {
                drawLine(tint, Offset(u * 0.22f, u * 0.72f), Offset(u * 0.76f, u * 0.18f), stroke, cap = cap)
                drawLine(tint, Offset(u * 0.50f, u * 0.18f), Offset(u * 0.76f, u * 0.18f), stroke, cap = cap)
                drawLine(tint, Offset(u * 0.76f, u * 0.18f), Offset(u * 0.76f, u * 0.44f), stroke, cap = cap)
            }

            LauncherSearchCategory.CONTACT -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.16f,
                    center = Offset(u * 0.50f, u * 0.34f),
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(u * 0.22f, u * 0.49f),
                    size = Size(u * 0.56f, u * 0.36f),
                    style = Stroke(width = stroke),
                )
            }

            LauncherSearchCategory.CALL_HISTORY -> {
                drawArc(
                    color = tint,
                    startAngle = 30f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = Offset(u * 0.17f, u * 0.17f),
                    size = Size(u * 0.66f, u * 0.66f),
                    style = Stroke(width = stroke, cap = cap),
                )
                drawLine(tint, Offset(u * 0.50f, u * 0.50f), Offset(u * 0.50f, u * 0.28f), stroke, cap = cap)
                drawLine(tint, Offset(u * 0.50f, u * 0.50f), Offset(u * 0.68f, u * 0.58f), stroke, cap = cap)
            }

            LauncherSearchCategory.MESSAGE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.12f, u * 0.20f),
                    size = Size(u * 0.76f, u * 0.54f),
                    cornerRadius = CornerRadius(u * 0.14f),
                    style = Stroke(width = stroke),
                )
                drawLine(tint, Offset(u * 0.30f, u * 0.74f), Offset(u * 0.24f, u * 0.86f), stroke, cap = cap)
                listOf(0.36f, 0.50f, 0.64f).forEach { x ->
                    drawCircle(color = tint, radius = u * 0.035f, center = Offset(u * x, u * 0.47f))
                }
            }

            LauncherSearchCategory.FILE -> {
                val path = Path().apply {
                    moveTo(u * 0.20f, u * 0.16f)
                    lineTo(u * 0.52f, u * 0.16f)
                    lineTo(u * 0.80f, u * 0.42f)
                    lineTo(u * 0.80f, u * 0.84f)
                    lineTo(u * 0.20f, u * 0.84f)
                    close()
                }
                drawPath(path, color = tint, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawLine(tint, Offset(u * 0.52f, u * 0.16f), Offset(u * 0.52f, u * 0.42f), stroke)
                drawLine(tint, Offset(u * 0.52f, u * 0.42f), Offset(u * 0.80f, u * 0.42f), stroke)
            }

            LauncherSearchCategory.CONNECTED_SOURCE -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.20f,
                    center = Offset(u * 0.40f, u * 0.46f),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.16f,
                    center = Offset(u * 0.66f, u * 0.46f),
                    style = Stroke(width = stroke),
                )
                drawLine(tint, Offset(u * 0.46f, u * 0.46f), Offset(u * 0.60f, u * 0.46f), stroke, cap = cap)
            }

            LauncherSearchCategory.SETTING -> {
                val center = Offset(u * 0.50f, u * 0.50f)
                drawCircle(color = tint, radius = u * 0.22f, center = center, style = Stroke(width = stroke))
                drawCircle(color = tint, radius = u * 0.06f, center = center, style = Stroke(width = stroke))
                listOf(
                    0.50f to 0.12f,
                    0.88f to 0.50f,
                    0.50f to 0.88f,
                    0.12f to 0.50f,
                ).forEach { (x, y) ->
                    val dx = x - 0.50f
                    val dy = y - 0.50f
                    drawLine(
                        tint,
                        Offset(u * (0.50f + dx * 0.66f), u * (0.50f + dy * 0.66f)),
                        Offset(u * x, u * y),
                        stroke,
                        cap = cap,
                    )
                }
            }
        }
    }
}
