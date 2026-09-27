package com.goreecloud.clock.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goreecloud.clock.data.ClockFacePreference
import com.goreecloud.clock.data.ClockPreferences
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class PresentationMode {
    NORMAL,
    FULL_SCREEN,
    BEDSIDE,
}

@Composable
fun ClockScreen(
    modifier: Modifier = Modifier,
    preferences: ClockPreferences,
    onClockFaceChanged: (ClockFacePreference) -> Unit,
    onPresentationModeChanged: (Boolean, Boolean) -> Unit,
) {
    var mode by remember { mutableStateOf(PresentationMode.NORMAL) }

    DisposableEffect(mode) {
        onPresentationModeChanged(
            mode != PresentationMode.NORMAL,
            mode == PresentationMode.BEDSIDE,
        )
        onDispose {
            if (mode != PresentationMode.NORMAL) {
                onPresentationModeChanged(false, false)
            }
        }
    }

    if (mode != PresentationMode.NORMAL) {
        BackHandler { mode = PresentationMode.NORMAL }
        FullClock(
            preferences = preferences,
            bedside = mode == PresentationMode.BEDSIDE,
            onExit = { mode = PresentationMode.NORMAL },
        )
        return
    }

    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = ZonedDateTime.now()
            delay(250L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = preferences.clockFace == ClockFacePreference.DIGITAL,
                onClick = { onClockFaceChanged(ClockFacePreference.DIGITAL) },
                label = { Text("Digital") },
            )
            FilterChip(
                selected = preferences.clockFace == ClockFacePreference.ANALOG,
                onClick = { onClockFaceChanged(ClockFacePreference.ANALOG) },
                label = { Text("Analog") },
            )
        }

        Spacer(Modifier.height(6.dp))
        ClockFace(
            now = now,
            use24Hour = preferences.use24Hour,
            face = preferences.clockFace,
        )
        Text(
            text = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = now.zone.id,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { mode = PresentationMode.FULL_SCREEN },
        ) {
            Text("Full-screen clock")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { mode = PresentationMode.BEDSIDE },
        ) {
            Text("Bedside mode")
        }
    }
}

@Composable
private fun FullClock(
    preferences: ClockPreferences,
    bedside: Boolean,
    onExit: () -> Unit,
) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = ZonedDateTime.now()
            delay(250L)
        }
    }

    val background = if (bedside) Color.Black else MaterialTheme.colorScheme.background
    val foreground = if (bedside) Color(0xFFE7F4F2) else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(28.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ClockFace(
                now = now,
                use24Hour = preferences.use24Hour,
                face = preferences.clockFace,
                forcedColor = foreground,
                large = true,
            )
            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.headlineSmall,
                color = foreground,
                textAlign = TextAlign.Center,
            )
        }
        OutlinedButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick = onExit,
        ) {
            Text("Exit")
        }
    }
}

@Composable
private fun ClockFace(
    now: ZonedDateTime,
    use24Hour: Boolean,
    face: ClockFacePreference,
    forcedColor: Color? = null,
    large: Boolean = false,
) {
    val color = forcedColor ?: MaterialTheme.colorScheme.onBackground
    if (face == ClockFacePreference.DIGITAL) {
        val pattern = if (use24Hour) "HH:mm:ss" else "h:mm:ss a"
        Text(
            modifier = Modifier.semantics { heading() },
            text = now.format(DateTimeFormatter.ofPattern(pattern)),
            style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
            color = color,
            textAlign = TextAlign.Center,
        )
    } else {
        AnalogClock(
            now = now,
            color = color,
            modifier = Modifier.size(if (large) 320.dp else 260.dp),
        )
    }
}

@Composable
private fun AnalogClock(
    now: ZonedDateTime,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val diameter = min(size.width, size.height)
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = radius * 0.98f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )

        for (index in 0 until 12) {
            val angle = Math.toRadians(index * 30.0 - 90.0)
            val start = Offset(
                center.x + cos(angle).toFloat() * radius * 0.82f,
                center.y + sin(angle).toFloat() * radius * 0.82f,
            )
            val end = Offset(
                center.x + cos(angle).toFloat() * radius * 0.92f,
                center.y + sin(angle).toFloat() * radius * 0.92f,
            )
            drawLine(
                color,
                start,
                end,
                strokeWidth = if (index % 3 == 0) 5f else 3f,
                cap = StrokeCap.Round,
            )
        }

        fun hand(angleDegrees: Double, length: Float, width: Float, alpha: Float = 1f) {
            val angle = Math.toRadians(angleDegrees - 90.0)
            drawLine(
                color = color.copy(alpha = alpha),
                start = center,
                end = Offset(
                    center.x + cos(angle).toFloat() * radius * length,
                    center.y + sin(angle).toFloat() * radius * length,
                ),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }

        val second = now.second + now.nano / 1_000_000_000f
        val minute = now.minute + second / 60f
        val hour = (now.hour % 12) + minute / 60f
        hand(hour * 30.0, 0.5f, 10f)
        hand(minute * 6.0, 0.7f, 7f)
        hand(second * 6.0, 0.78f, 3f, 0.72f)
        drawCircle(color, radius = 8f, center = center)
    }
}
