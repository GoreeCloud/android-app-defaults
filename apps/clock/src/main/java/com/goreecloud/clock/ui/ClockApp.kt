package com.goreecloud.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goreecloud.clock.MainActivity
import com.goreecloud.clock.alarm.AlarmScheduler
import com.goreecloud.clock.alarm.AlarmStore
import com.goreecloud.clock.data.ClockPreferences
import com.goreecloud.clock.data.ClockHintIds
import com.goreecloud.clock.data.ClockPreferencesStore
import com.goreecloud.clock.data.ThemePreference
import com.goreecloud.clock.stopwatch.StopwatchStore
import com.goreecloud.clock.timer.TimerScheduler
import com.goreecloud.clock.timer.TimerStore
import com.goreecloud.clock.ui.screens.AlarmScreen
import com.goreecloud.clock.ui.screens.OnboardingScreen
import com.goreecloud.clock.ui.screens.ClockScreen
import com.goreecloud.clock.ui.screens.StopwatchScreen
import com.goreecloud.clock.ui.screens.TimerScreen
import com.goreecloud.clock.ui.screens.WorldClockScreen

private enum class ClockDestination(val label: String) {
    CLOCK("Clock"),
    ALARMS("Alarms"),
    TIMER("Timer"),
    STOPWATCH("Stopwatch"),
    WORLD("World"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockApp(
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
    alarmStore: AlarmStore,
    alarmScheduler: AlarmScheduler,
    timerStore: TimerStore,
    timerScheduler: TimerScheduler,
    stopwatchStore: StopwatchStore,
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
    destinationRequest: String?,
    onDestinationConsumed: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onPresentationModeChanged: (immersive: Boolean, keepScreenOn: Boolean) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(ClockDestination.CLOCK) }
    var showSettings by remember { mutableStateOf(false) }
    val onHaptic = rememberClockHapticFeedback(preferences.hapticsEnabled)

    if (!preferences.onboardingCompleted || preferences.onboardingReplay) {
        OnboardingScreen(
            preferences = preferences,
            preferencesStore = preferencesStore,
            exactAlarmAccess = exactAlarmAccess,
            notificationAccess = notificationAccess,
            replayMode = preferences.onboardingReplay,
            onCancelReplay = preferencesStore::cancelOnboardingReplay,
            onHaptic = onHaptic,
        )
        return
    }

    LaunchedEffect(destinationRequest) {
        val requested = when (destinationRequest) {
            MainActivity.DESTINATION_CLOCK -> ClockDestination.CLOCK
            MainActivity.DESTINATION_ALARMS -> ClockDestination.ALARMS
            MainActivity.DESTINATION_TIMER -> ClockDestination.TIMER
            else -> null
        }
        if (requested != null) {
            destination = requested
            onDestinationConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(destination.label) },
                actions = {
                    TextButton(onClick = {
                        onHaptic(ClockHapticEvent.ACTION)
                        showSettings = true
                    }) {
                        Text("Settings")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                ClockDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            onHaptic(ClockHapticEvent.TICK)
                            destination = item
                        },
                        icon = { ClockDestinationIcon(item) },
                    )
                }
            }
        },
    ) { padding ->
        DestinationContent(
            destination = destination,
            padding = padding,
            preferences = preferences,
            preferencesStore = preferencesStore,
            alarmStore = alarmStore,
            alarmScheduler = alarmScheduler,
            timerStore = timerStore,
            timerScheduler = timerScheduler,
            stopwatchStore = stopwatchStore,
            exactAlarmAccess = exactAlarmAccess,
            notificationAccess = notificationAccess,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestNotificationAccess = onRequestNotificationAccess,
            onDismissHint = preferencesStore::dismissHint,
            onHaptic = onHaptic,
            onPresentationModeChanged = onPresentationModeChanged,
        )
    }

    if (showSettings) {
        SettingsDialog(
            preferences = preferences,
            preferencesStore = preferencesStore,
            onDismiss = { showSettings = false },
            onHaptic = onHaptic,
        )
    }
}

@Composable
private fun DestinationContent(
    destination: ClockDestination,
    padding: PaddingValues,
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
    alarmStore: AlarmStore,
    alarmScheduler: AlarmScheduler,
    timerStore: TimerStore,
    timerScheduler: TimerScheduler,
    stopwatchStore: StopwatchStore,
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onDismissHint: (String) -> Unit,
    onHaptic: (ClockHapticEvent) -> Unit,
    onPresentationModeChanged: (Boolean, Boolean) -> Unit,
) {
    val modifier = Modifier.padding(padding)
    when (destination) {
        ClockDestination.CLOCK -> ClockScreen(
            modifier = modifier,
            preferences = preferences,
            onClockFaceChanged = preferencesStore::setClockFace,
            onHaptic = onHaptic,
            onPresentationModeChanged = onPresentationModeChanged,
        )
        ClockDestination.ALARMS -> AlarmScreen(
            modifier = modifier,
            use24Hour = preferences.use24Hour,
            alarmStore = alarmStore,
            scheduler = alarmScheduler,
            exactAlarmAccess = exactAlarmAccess,
            notificationAccess = notificationAccess,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestNotificationAccess = onRequestNotificationAccess,
            showHint = preferences.hintsEnabled && ClockHintIds.ALARMS_RELIABILITY !in preferences.dismissedHints,
            onDismissHint = { onDismissHint(ClockHintIds.ALARMS_RELIABILITY) },
            onHaptic = onHaptic,
        )
        ClockDestination.TIMER -> TimerScreen(
            modifier = modifier,
            timerStore = timerStore,
            scheduler = timerScheduler,
            exactAlarmAccess = exactAlarmAccess,
            notificationAccess = notificationAccess,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestNotificationAccess = onRequestNotificationAccess,
            showHint = preferences.hintsEnabled && ClockHintIds.TIMER_RELIABILITY !in preferences.dismissedHints,
            onDismissHint = { onDismissHint(ClockHintIds.TIMER_RELIABILITY) },
            onHaptic = onHaptic,
        )
        ClockDestination.STOPWATCH -> StopwatchScreen(
            modifier = modifier,
            store = stopwatchStore,
            reducedMotion = preferences.reducedMotion,
            use24Hour = preferences.use24Hour,
            onHaptic = onHaptic,
        )
        ClockDestination.WORLD -> WorldClockScreen(
            modifier = modifier,
            use24Hour = preferences.use24Hour,
            zones = preferences.worldZones,
            onZonesChanged = preferencesStore::setWorldZones,
            onHaptic = onHaptic,
        )
    }
}

@Composable
private fun SettingsDialog(
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
    onDismiss: () -> Unit,
    onHaptic: (ClockHapticEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clock settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                ThemePreference.entries.forEach { option ->
                    SelectionRow(
                        label = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = preferences.theme == option,
                        onClick = {
                            onHaptic(ClockHapticEvent.TICK)
                            preferencesStore.setTheme(option)
                        },
                    )
                }

                Spacer(Modifier.height(6.dp))
                ToggleRow(
                    label = "24-hour time",
                    checked = preferences.use24Hour,
                    onCheckedChange = {
                        onHaptic(ClockHapticEvent.ACTION)
                        preferencesStore.setUse24Hour(it)
                    },
                )
                ToggleRow(
                    label = "Haptic feedback",
                    checked = preferences.hapticsEnabled,
                    onCheckedChange = {
                        onHaptic(ClockHapticEvent.ACTION)
                        preferencesStore.setHapticsEnabled(it)
                    },
                )
                ToggleRow(
                    label = "Reduced motion",
                    checked = preferences.reducedMotion,
                    onCheckedChange = {
                        onHaptic(ClockHapticEvent.ACTION)
                        preferencesStore.setReducedMotion(it)
                    },
                )
                ToggleRow(
                    label = "Show widget details",
                    checked = preferences.showWidgetDetails,
                    onCheckedChange = {
                        onHaptic(ClockHapticEvent.ACTION)
                        preferencesStore.setShowWidgetDetails(it)
                    },
                )
                ToggleRow(
                    label = "Contextual hints",
                    checked = preferences.hintsEnabled,
                    onCheckedChange = {
                        onHaptic(ClockHapticEvent.ACTION)
                        preferencesStore.setHintsEnabled(it)
                    },
                )
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onHaptic(ClockHapticEvent.ACTION)
                        preferencesStore.resetDismissedHints()
                    },
                ) {
                    Text("Reset dismissed hints")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onHaptic(ClockHapticEvent.ACTION)
                        onDismiss()
                        preferencesStore.replayOnboarding()
                    },
                ) {
                    Text("Replay onboarding")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onHaptic(ClockHapticEvent.ACTION)
                onDismiss()
            }) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun SelectionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Composable
private fun ClockDestinationIcon(destination: ClockDestination) {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = destination.label },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = size.minDimension * 0.075f
        val stroke = Stroke(width = strokeWidth)
        val radius = size.minDimension * 0.34f

        when (destination) {
            ClockDestination.CLOCK -> {
                drawCircle(color = color, radius = radius, center = center, style = stroke)
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.58f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x + radius * 0.52f, center.y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            ClockDestination.ALARMS -> {
                val alarmCenter = Offset(center.x, center.y + size.height * 0.04f)
                val alarmRadius = radius * 0.82f
                drawCircle(color = color, radius = alarmRadius, center = alarmCenter, style = stroke)
                drawLine(
                    color = color,
                    start = alarmCenter,
                    end = Offset(alarmCenter.x, alarmCenter.y - alarmRadius * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = alarmCenter,
                    end = Offset(alarmCenter.x + alarmRadius * 0.46f, alarmCenter.y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(center.x - alarmRadius * 0.78f, center.y - alarmRadius * 0.82f),
                    end = Offset(center.x - alarmRadius * 0.35f, center.y - alarmRadius * 1.14f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(center.x + alarmRadius * 0.78f, center.y - alarmRadius * 0.82f),
                    end = Offset(center.x + alarmRadius * 0.35f, center.y - alarmRadius * 1.14f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(center.x - alarmRadius * 0.58f, center.y + alarmRadius * 0.8f),
                    end = Offset(center.x - alarmRadius * 0.82f, center.y + alarmRadius * 1.08f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(center.x + alarmRadius * 0.58f, center.y + alarmRadius * 0.8f),
                    end = Offset(center.x + alarmRadius * 0.82f, center.y + alarmRadius * 1.08f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            ClockDestination.TIMER -> {
                val left = center.x - radius * 0.72f
                val right = center.x + radius * 0.72f
                val top = center.y - radius
                val bottom = center.y + radius
                drawLine(color, Offset(left, top), Offset(right, top), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(left, bottom), Offset(right, bottom), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(left, top), center, strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(right, top), center, strokeWidth, StrokeCap.Round)
                drawLine(color, center, Offset(left, bottom), strokeWidth, StrokeCap.Round)
                drawLine(color, center, Offset(right, bottom), strokeWidth, StrokeCap.Round)
            }

            ClockDestination.STOPWATCH -> {
                val watchCenter = Offset(center.x, center.y + size.height * 0.05f)
                val watchRadius = radius * 0.9f
                drawCircle(color = color, radius = watchRadius, center = watchCenter, style = stroke)
                drawLine(
                    color,
                    Offset(center.x, watchCenter.y - watchRadius),
                    Offset(center.x, watchCenter.y - watchRadius * 1.38f),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(center.x - watchRadius * 0.35f, watchCenter.y - watchRadius * 1.38f),
                    Offset(center.x + watchRadius * 0.35f, watchCenter.y - watchRadius * 1.38f),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(center.x + watchRadius * 0.72f, watchCenter.y - watchRadius * 0.72f),
                    Offset(center.x + watchRadius * 1.04f, watchCenter.y - watchRadius * 1.02f),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    watchCenter,
                    Offset(watchCenter.x + watchRadius * 0.42f, watchCenter.y - watchRadius * 0.46f),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }

            ClockDestination.WORLD -> {
                drawCircle(color = color, radius = radius, center = center, style = stroke)
                drawLine(
                    color,
                    Offset(center.x - radius, center.y),
                    Offset(center.x + radius, center.y),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(center.x, center.y - radius),
                    Offset(center.x, center.y + radius),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawCircle(
                    color = color,
                    radius = radius * 0.5f,
                    center = center,
                    style = Stroke(width = strokeWidth * 0.8f),
                )
            }
        }
    }
}
