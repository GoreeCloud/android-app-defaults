package com.goreecloud.clock.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.clock.stopwatch.StopwatchResult
import com.goreecloud.clock.stopwatch.StopwatchStore
import com.goreecloud.clock.timer.DurationFormatter
import com.goreecloud.clock.ui.ClockHapticEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun StopwatchScreen(
    modifier: Modifier = Modifier,
    store: StopwatchStore,
    reducedMotion: Boolean,
    use24Hour: Boolean,
    onHaptic: (ClockHapticEvent) -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    val history by store.history.collectAsStateWithLifecycle()
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    LaunchedEffect(state.running, reducedMotion) {
        tick = SystemClock.elapsedRealtime()
        while (isActive && state.running) {
            tick = SystemClock.elapsedRealtime()
            delay(if (reducedMotion) 250L else 50L)
        }
    }

    val elapsed = state.elapsedAt(elapsedRealtime = tick)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            DurationFormatter.format(elapsed, showHundredths = true),
            style = MaterialTheme.typography.displayMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.running) {
                Button(onClick = {
                    onHaptic(ClockHapticEvent.ACTION)
                    store.pause()
                }) { Text("Pause") }
                OutlinedButton(onClick = {
                    onHaptic(ClockHapticEvent.TICK)
                    store.lap()
                }) { Text("Lap") }
            } else {
                Button(onClick = {
                    onHaptic(ClockHapticEvent.ACTION)
                    store.start()
                }) {
                    Text(if (elapsed > 0L) "Resume" else "Start")
                }
            }
            OutlinedButton(onClick = {
                onHaptic(ClockHapticEvent.ACTION)
                store.reset()
            }) { Text("Reset") }
        }

        Text("Laps", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.laps.isEmpty()) {
                item {
                    Text(
                        "No laps yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(state.laps.asReversed()) { reverseIndex, lapTotal ->
                val originalIndex = state.laps.lastIndex - reverseIndex
                val previous = if (originalIndex == 0) 0L else state.laps[originalIndex - 1]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Lap ${originalIndex + 1}")
                        Text(DurationFormatter.format(lapTotal - previous, showHundredths = true))
                        Text(DurationFormatter.format(lapTotal, showHundredths = true))
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Recent results", style = MaterialTheme.typography.titleLarge)
                        TextButton(
                            onClick = {
                                onHaptic(ClockHapticEvent.ACTION)
                                confirmClearHistory = true
                            },
                        ) { Text("Clear all") }
                    }
                }

                items(history, key = StopwatchResult::id) { result ->
                    StopwatchResultCard(
                        result = result,
                        use24Hour = use24Hour,
                        onDelete = {
                            onHaptic(ClockHapticEvent.ACTION)
                            store.deleteResult(result.id)
                        },
                    )
                }
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear stopwatch history?") },
            text = {
                Text(
                    "This permanently removes all saved stopwatch results from this device. " +
                        "The current stopwatch and laps are not changed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHaptic(ClockHapticEvent.ACTION)
                        store.clearHistory()
                        confirmClearHistory = false
                    },
                ) {
                    Text("Clear history")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun StopwatchResultCard(
    result: StopwatchResult,
    use24Hour: Boolean,
    onDelete: () -> Unit,
) {
    val finished = remember(result.finishedAtWallMillis, use24Hour) {
        val pattern = if (use24Hour) "MMM d, yyyy • HH:mm" else "MMM d, yyyy • h:mm a"
        Instant.ofEpochMilli(result.finishedAtWallMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    DurationFormatter.format(result.elapsedMillis, showHundredths = true),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    finished,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (result.laps.isEmpty()) "No laps" else "${result.laps.size} laps",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
