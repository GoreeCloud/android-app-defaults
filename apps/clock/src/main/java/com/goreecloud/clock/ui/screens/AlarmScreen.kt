package com.goreecloud.clock.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.clock.alarm.Alarm
import com.goreecloud.clock.alarm.AlarmScheduler
import com.goreecloud.clock.alarm.AlarmStore
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
) {
    val alarms by alarmStore.alarms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!exactAlarmAccess) {
            PermissionCard(
                title = "Exact alarm access is required",
                body = "Android requires special access for dependable alarm and timer delivery.",
                button = "Allow exact alarms",
                onClick = onRequestExactAlarmAccess,
            )
        }
        if (!notificationAccess) {
            PermissionCard(
                title = "Notifications are disabled",
                body = "Allow notifications so alarms and timer completions can alert you.",
                button = "Allow notifications",
                onClick = onRequestNotificationAccess,
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { adding = true },
        ) {
            Text("Add alarm")
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
                    onEdit = { editing = alarm },
                    onEnabledChange = { enabled ->
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
            onSave = { draft ->
                val saved = alarmStore.add(
                    hour = draft.hour,
                    minute = draft.minute,
                    label = draft.label,
                    repeatDays = draft.repeatDays,
                    vibrate = draft.vibrate,
                    snoozeMinutes = draft.snoozeMinutes,
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
            onSave = { updated ->
                alarmStore.upsert(updated)
                if (updated.enabled) scheduler.schedule(updated) else scheduler.cancel(updated.id)
                editing = null
            },
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
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
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
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
