package com.goreecloud.clock.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.clock.stopwatch.StopwatchStore
import com.goreecloud.clock.timer.DurationFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun StopwatchScreen(
    modifier: Modifier = Modifier,
    store: StopwatchStore,
    reducedMotion: Boolean,
) {
    val state by store.state.collectAsStateWithLifecycle()
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

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
                Button(onClick = { store.pause() }) { Text("Pause") }
                OutlinedButton(onClick = { store.lap() }) { Text("Lap") }
            } else {
                Button(onClick = { store.start() }) {
                    Text(if (elapsed > 0L) "Resume" else "Start")
                }
            }
            OutlinedButton(onClick = { store.reset() }) { Text("Reset") }
        }

        Text("Laps", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
    }
}
