package com.goreecloud.clock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.clock.alarm.Alarm
import com.goreecloud.clock.alarm.AlarmScheduler
import com.goreecloud.clock.alarm.AlarmSound
import com.goreecloud.clock.alarm.AlarmSoundCatalog
import com.goreecloud.clock.alarm.AlarmSoundOption
import com.goreecloud.clock.alarm.AlarmStore
import com.goreecloud.clock.alarm.UpcomingAlarmPolicy
import com.goreecloud.clock.alarm.UpcomingAlarmPresentationPolicy
import com.goreecloud.clock.ui.ClockHapticEvent
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    use24Hour: Boolean,
    alarmStore: AlarmStore,
    scheduler: AlarmScheduler,
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    showHint: Boolean,
    onDismissHint: () -> Unit,
    onHaptic: (ClockHapticEvent) -> Unit,
) {
    val alarms by alarmStore.alarms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var adding by remember { mutableStateOf(false) }
    var scheduleNow by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000L)
            scheduleNow = ZonedDateTime.now()
        }
    }
    val upcomingAlarm = remember(alarms, scheduleNow) {
        UpcomingAlarmPolicy.next(alarms, scheduleNow)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onHaptic(ClockHapticEvent.ACTION)
                adding = true
            },
        ) {
            Text("Add alarm")
        }

        if (showHint) {
            ContextualHintCard(
                title = "Reliable alarms, only when you need them",
                body = "Alarm data stays on this device. Android notification and exact-alarm access are requested only for dependable alert delivery.",
                onDismiss = {
                    onHaptic(ClockHapticEvent.ACTION)
                    onDismissHint()
                },
            )
        }

        if (!exactAlarmAccess) {
            PermissionCard(
                title = "Exact alarm access is required",
                body = "Android requires special access for dependable alarm and timer delivery.",
                button = "Allow exact alarms",
                onClick = {
                    onHaptic(ClockHapticEvent.ACTION)
                    onRequestExactAlarmAccess()
                },
            )
        }
        if (!notificationAccess) {
            PermissionCard(
                title = "Notifications are disabled",
                body = "Allow notifications so alarms and timer completions can alert you.",
                button = "Allow notifications",
                onClick = {
                    onHaptic(ClockHapticEvent.ACTION)
                    onRequestNotificationAccess()
                },
            )
        }

        if (exactAlarmAccess) {
            upcomingAlarm?.let { upcoming ->
                UpcomingAlarmCard(
                    alarm = upcoming.alarm,
                    trigger = upcoming.trigger,
                    now = scheduleNow,
                    use24Hour = use24Hour,
                    onEdit = {
                        onHaptic(ClockHapticEvent.ACTION)
                        editing = upcoming.alarm
                    },
                )
            }
        }

        if (alarms.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("No alarms yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Create an alarm. All alarm data stays on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    use24Hour = use24Hour,
                    onEdit = {
                        onHaptic(ClockHapticEvent.ACTION)
                        editing = alarm
                    },
                    onEnabledChange = { enabled ->
                        onHaptic(ClockHapticEvent.ACTION)
                        val updated = alarmStore.setEnabled(alarm.id, enabled)
                        if (updated != null) {
                            if (enabled) {
                                scheduler.schedule(updated)
                            } else {
                                scheduler.cancel(alarm.id)
                            }
                        }
                    },
                    onDelete = {
                        onHaptic(ClockHapticEvent.ACTION)
                        scheduler.cancel(alarm.id)
                        alarmStore.delete(alarm.id)
                    },
                )
            }
        }
    }

    if (adding) {
        AlarmEditorDialog(
            existing = null,
            onDismiss = { adding = false },
            onHaptic = onHaptic,
            onSave = { draft ->
                onHaptic(ClockHapticEvent.ACTION)
                val saved = alarmStore.add(
                    hour = draft.hour,
                    minute = draft.minute,
                    label = draft.label,
                    repeatDays = draft.repeatDays,
                    vibrate = draft.vibrate,
                    snoozeMinutes = draft.snoozeMinutes,
                    soundKey = draft.soundKey,
                    gradualVolumeSeconds = draft.gradualVolumeSeconds,
                )
                scheduler.schedule(saved)
                adding = false
            },
        )
    }

    editing?.let { alarm ->
        AlarmEditorDialog(
            existing = alarm,
            onDismiss = { editing = null },
            onHaptic = onHaptic,
            onSave = { updated ->
                onHaptic(ClockHapticEvent.ACTION)
                alarmStore.upsert(updated)
                if (updated.enabled) scheduler.schedule(updated) else scheduler.cancel(updated.id)
                editing = null
            },
        )
    }
}

@Composable
private fun UpcomingAlarmCard(
    alarm: Alarm,
    trigger: ZonedDateTime,
    now: ZonedDateTime,
    use24Hour: Boolean,
    onEdit: () -> Unit,
) {
    val dayLabel = when (trigger.toLocalDate()) {
        now.toLocalDate() -> "Today"
        now.toLocalDate().plusDays(1) -> "Tomorrow"
        else -> trigger.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    val time = trigger.format(
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a"),
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("next-alarm-card")
            .clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "Next alarm",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "$dayLabel · $time",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                UpcomingAlarmPresentationPolicy.relativeSummary(now, trigger),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                UpcomingAlarmPresentationPolicy.repeatSummary(alarm.repeatDays),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                alarm.label.ifBlank { "Alarm" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    use24Hour: Boolean,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val time = LocalTime.of(alarm.hour, alarm.minute).format(
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a"),
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(time, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        alarm.label.ifBlank { "Alarm" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = onEnabledChange)
            }

            val repeat = if (alarm.repeatDays.isEmpty()) {
                "One time"
            } else {
                alarm.repeatDays.sortedBy { it.value }.joinToString(" • ") {
                    it.name.take(3).lowercase().replaceFirstChar(Char::uppercase)
                }
            }
            Text(
                "$repeat • Snooze ${alarm.snoozeMinutes} min",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val gradual = if (alarm.gradualVolumeSeconds > 0) {
                "Gradual ${alarm.gradualVolumeSeconds}s"
            } else {
                "Full volume"
            }
            Text(
                "Sound: ${AlarmSoundCatalog.title(context, alarm.soundKey)} • $gradual",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun AlarmEditorDialog(
    existing: Alarm?,
    onDismiss: () -> Unit,
    onHaptic: (ClockHapticEvent) -> Unit,
    onSave: (Alarm) -> Unit,
) {
    val now = LocalTime.now()
    var hour by remember(existing?.id) {
        mutableStateOf((existing?.hour ?: now.hour).toString())
    }
    var minute by remember(existing?.id) {
        mutableStateOf((existing?.minute ?: now.minute).toString().padStart(2, '0'))
    }
    var label by remember(existing?.id) { mutableStateOf(existing?.label.orEmpty()) }
    var repeatDays by remember(existing?.id) {
        mutableStateOf(existing?.repeatDays ?: emptySet())
    }
    var vibrate by remember(existing?.id) { mutableStateOf(existing?.vibrate ?: true) }
    var snooze by remember(existing?.id) {
        mutableStateOf((existing?.snoozeMinutes ?: 10).toString())
    }
    val context = LocalContext.current
    var soundKey by remember(existing?.id) {
        mutableStateOf(existing?.soundKey ?: AlarmSound.DEFAULT)
    }
    var gradualVolumeSeconds by remember(existing?.id) {
        mutableStateOf(existing?.gradualVolumeSeconds ?: 0)
    }
    var showSoundPicker by remember { mutableStateOf(false) }

    val parsedHour = hour.toIntOrNull()
    val parsedMinute = minute.toIntOrNull()
    val parsedSnooze = snooze.toIntOrNull()
    val valid = parsedHour != null && parsedHour in 0..23 &&
        parsedMinute != null && parsedMinute in 0..59 &&
        parsedSnooze != null && parsedSnooze in 1..60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New alarm" else "Edit alarm") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = hour,
                        onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = minute,
                        onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label,
                    onValueChange = { label = it.take(80) },
                    label = { Text("Label") },
                    singleLine = true,
                )
                Text("Repeat", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DayOfWeek.entries) { day ->
                        FilterChip(
                            selected = day in repeatDays,
                            onClick = {
                                onHaptic(ClockHapticEvent.TICK)
                                repeatDays = if (day in repeatDays) {
                                    repeatDays - day
                                } else {
                                    repeatDays + day
                                }
                            },
                            label = { Text(day.name.take(2)) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Vibrate")
                    Switch(
                        checked = vibrate,
                        onCheckedChange = {
                            onHaptic(ClockHapticEvent.ACTION)
                            vibrate = it
                        },
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onHaptic(ClockHapticEvent.ACTION)
                        showSoundPicker = true
                    },
                ) {
                    Text("Alarm sound: ${AlarmSoundCatalog.title(context, soundKey)}")
                }
                Text("Gradual volume", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(0, 15, 30, 60)) { seconds ->
                        FilterChip(
                            selected = gradualVolumeSeconds == seconds,
                            onClick = {
                                onHaptic(ClockHapticEvent.TICK)
                                gradualVolumeSeconds = seconds
                            },
                            label = { Text(if (seconds == 0) "Off" else "${seconds}s") },
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = snooze,
                    onValueChange = { snooze = it.filter(Char::isDigit).take(2) },
                    label = { Text("Snooze minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        Alarm(
                            id = existing?.id ?: -1L,
                            hour = requireNotNull(parsedHour),
                            minute = requireNotNull(parsedMinute),
                            label = label.trim(),
                            enabled = existing?.enabled ?: true,
                            repeatDays = repeatDays,
                            vibrate = vibrate,
                            snoozeMinutes = requireNotNull(parsedSnooze),
                            soundKey = soundKey,
                            gradualVolumeSeconds = gradualVolumeSeconds,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onHaptic(ClockHapticEvent.ACTION)
                    onDismiss()
                },
            ) {
                Text("Cancel")
            }
        },
    )

    if (showSoundPicker) {
        AlarmSoundPickerDialog(
            selectedKey = soundKey,
            onHaptic = onHaptic,
            onSelected = {
                onHaptic(ClockHapticEvent.TICK)
                soundKey = it
                showSoundPicker = false
            },
            onDismiss = { showSoundPicker = false },
        )
    }
}

@Composable
private fun AlarmSoundPickerDialog(
    selectedKey: String,
    onHaptic: (ClockHapticEvent) -> Unit,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val options = remember { AlarmSoundCatalog.load(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose alarm sound") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(options, key = AlarmSoundOption::key) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.key == selectedKey,
                            onClick = { onSelected(option.key) },
                        )
                        Text(option.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onHaptic(ClockHapticEvent.ACTION)
                    onDismiss()
                },
            ) { Text("Done") }
        },
    )
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onClick) {
                Text(button)
            }
        }
    }
}
