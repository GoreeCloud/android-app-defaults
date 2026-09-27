package com.goreecloud.clock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goreecloud.clock.MainActivity
import com.goreecloud.clock.alarm.AlarmScheduler
import com.goreecloud.clock.alarm.AlarmStore
import com.goreecloud.clock.data.ClockPreferences
import com.goreecloud.clock.data.ClockPreferencesStore
import com.goreecloud.clock.data.ThemePreference
import com.goreecloud.clock.stopwatch.StopwatchStore
import com.goreecloud.clock.timer.TimerScheduler
import com.goreecloud.clock.timer.TimerStore
import com.goreecloud.clock.ui.screens.AlarmScreen
import com.goreecloud.clock.ui.screens.ClockScreen
import com.goreecloud.clock.ui.screens.StopwatchScreen
import com.goreecloud.clock.ui.screens.TimerScreen
import com.goreecloud.clock.ui.screens.WorldClockScreen

private enum class ClockDestination(val label: String, val glyph: String) {
    CLOCK("Clock", "◷"),
    ALARMS("Alarms", "⏰"),
    TIMER("Timer", "⌛"),
    STOPWATCH("Stopwatch", "⏱"),
    WORLD("World", "◎"),
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
                    TextButton(onClick = { showSettings = true }) {
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
                        onClick = { destination = item },
                        icon = { Text(item.glyph) },
                        label = { Text(item.label) },
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
            onPresentationModeChanged = onPresentationModeChanged,
        )
    }

    if (showSettings) {
        SettingsDialog(
            preferences = preferences,
            preferencesStore = preferencesStore,
            onDismiss = { showSettings = false },
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
    onPresentationModeChanged: (Boolean, Boolean) -> Unit,
) {
    val modifier = Modifier.padding(padding)
    when (destination) {
        ClockDestination.CLOCK -> ClockScreen(
            modifier = modifier,
            preferences = preferences,
            onClockFaceChanged = preferencesStore::setClockFace,
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
        )
        ClockDestination.TIMER -> TimerScreen(
            modifier = modifier,
            timerStore = timerStore,
            scheduler = timerScheduler,
            exactAlarmAccess = exactAlarmAccess,
            notificationAccess = notificationAccess,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestNotificationAccess = onRequestNotificationAccess,
        )
        ClockDestination.STOPWATCH -> StopwatchScreen(
            modifier = modifier,
            store = stopwatchStore,
            reducedMotion = preferences.reducedMotion,
        )
        ClockDestination.WORLD -> WorldClockScreen(
            modifier = modifier,
            use24Hour = preferences.use24Hour,
            zones = preferences.worldZones,
            onZonesChanged = preferencesStore::setWorldZones,
        )
    }
}

@Composable
private fun SettingsDialog(
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clock settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                ThemePreference.entries.forEach { option ->
                    SelectionRow(
                        label = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = preferences.theme == option,
                        onClick = { preferencesStore.setTheme(option) },
                    )
                }

                Spacer(Modifier.height(6.dp))
                ToggleRow(
                    label = "24-hour time",
                    checked = preferences.use24Hour,
                    onCheckedChange = preferencesStore::setUse24Hour,
                )
                ToggleRow(
                    label = "Haptic feedback",
                    checked = preferences.hapticsEnabled,
                    onCheckedChange = preferencesStore::setHapticsEnabled,
                )
                ToggleRow(
                    label = "Reduced motion",
                    checked = preferences.reducedMotion,
                    onCheckedChange = preferencesStore::setReducedMotion,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
