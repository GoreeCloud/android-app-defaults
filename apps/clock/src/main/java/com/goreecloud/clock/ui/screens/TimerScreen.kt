package com.goreecloud.clock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.clock.timer.DurationFormatter
import com.goreecloud.clock.timer.TimerEntry
import com.goreecloud.clock.timer.TimerScheduler
import com.goreecloud.clock.timer.TimerStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    timerStore: TimerStore,
    scheduler: TimerScheduler,
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
) {
    val timers by timerStore.timers.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(250L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!exactAlarmAccess) {
            TimerPermissionCard(
                title = "Exact alarm access is required",
                button = "Allow exact alarms",
                onClick = onRequestExactAlarmAccess,
            )
        }
        if (!notificationAccess) {
            TimerPermissionCard(
                title = "Notifications are disabled",
                button = "Allow notifications",
                onClick = onRequestNotificationAccess,
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showAdd = true },
        ) {
            Text("Add timer")
        }

        if (timers.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("No timers", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Create multiple independent timers. Running timers are restored after app recreation.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    now = now,
                    onStart = {
                        timerStore.start(timer.id)?.let(scheduler::schedule)
                    },
                    onPause = {
                        timerStore.pause(timer.id)
                        scheduler.cancel(timer.id)
                    },
                    onReset = {
                        timerStore.reset(timer.id)
                        scheduler.cancel(timer.id)
                    },
                    onDelete = {
                        scheduler.cancel(timer.id)
                        timerStore.delete(timer.id)
                    },
                )
            }
        }
    }

    if (showAdd) {
        AddTimerDialog(
            onDismiss = { showAdd = false },
            onAdd = { label, duration ->
                timerStore.add(label, duration)
                showAdd = false
            },
        )
    }
}

@Composable
private fun TimerCard(
    timer: TimerEntry,
    now: Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val remaining = timer.remainingAt(now)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                timer.label.ifBlank { "Timer" },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                DurationFormatter.format(remaining),
                style = MaterialTheme.typography.displaySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (timer.running) {
                    Button(onClick = onPause) { Text("Pause") }
                } else {
                    Button(onClick = onStart) {
                        Text(if (remaining == 0L) "Restart" else "Start")
                    }
                }
                OutlinedButton(onClick = onReset) { Text("Reset") }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun AddTimerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("5") }
    var seconds by remember { mutableStateOf("0") }

    val h = hours.toLongOrNull() ?: -1L
    val m = minutes.toLongOrNull() ?: -1L
    val s = seconds.toLongOrNull() ?: -1L
    val duration = if (h >= 0L && m in 0L..59L && s in 0L..59L) {
        ((h * 3_600L) + (m * 60L) + s) * 1_000L
    } else {
        0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = label,
                    onValueChange = { label = it.take(80) },
                    label = { Text("Label") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationField(Modifier.weight(1f), "Hours", hours) { hours = it }
                    DurationField(Modifier.weight(1f), "Minutes", minutes) { minutes = it }
                    DurationField(Modifier.weight(1f), "Seconds", seconds) { seconds = it }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = duration > 0L,
                onClick = { onAdd(label.trim(), duration) },
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DurationField(
    modifier: Modifier,
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun TimerPermissionCard(
    title: String,
    button: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, modifier = Modifier.weight(1f))
            TextButton(onClick = onClick) { Text(button) }
        }
    }
}
