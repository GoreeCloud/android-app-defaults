package com.goreecloud.clock.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.goreecloud.clock.data.WorldClockOrderPolicy
import com.goreecloud.clock.ui.ClockHapticEvent
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class WorldCity(val label: String, val zoneId: String)

private val AvailableCities = listOf(
    WorldCity("UTC", "UTC"),
    WorldCity("New York", "America/New_York"),
    WorldCity("Chicago", "America/Chicago"),
    WorldCity("Los Angeles", "America/Los_Angeles"),
    WorldCity("London", "Europe/London"),
    WorldCity("Paris", "Europe/Paris"),
    WorldCity("Lagos", "Africa/Lagos"),
    WorldCity("Dubai", "Asia/Dubai"),
    WorldCity("Delhi", "Asia/Kolkata"),
    WorldCity("Singapore", "Asia/Singapore"),
    WorldCity("Tokyo", "Asia/Tokyo"),
    WorldCity("Sydney", "Australia/Sydney"),
)

@Composable
fun WorldClockScreen(
    modifier: Modifier = Modifier,
    use24Hour: Boolean,
    zones: List<String>,
    onZonesChanged: (List<String>) -> Unit,
    onHaptic: (ClockHapticEvent) -> Unit,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    var showAdd by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = Instant.now()
            delay(1_000L)
        }
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
                showAdd = true
            },
        ) {
            Text("Add world clock")
        }

        val localZone = ZoneId.systemDefault()
        WorldClockCard(
            title = "Local",
            zoneId = localZone.id,
            instant = now,
            use24Hour = use24Hour,
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(zones, key = { _, zone -> zone }) { index, zone ->
                val city = AvailableCities.firstOrNull { it.zoneId == zone }?.label ?: zone
                val tag = zone.replace('/', '_')
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("world-clock-$tag"),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WorldClockCardContent(
                            title = city,
                            zoneId = zone,
                            instant = now,
                            use24Hour = use24Hour,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TextButton(
                                modifier = Modifier.testTag("world-clock-move-up-$tag"),
                                enabled = index > 0,
                                onClick = {
                                    onHaptic(ClockHapticEvent.TICK)
                                    onZonesChanged(
                                        WorldClockOrderPolicy.move(
                                            zones = zones,
                                            index = index,
                                            offset = -1,
                                        )
                                    )
                                },
                            ) { Text("Move up") }
                            TextButton(
                                modifier = Modifier.testTag("world-clock-move-down-$tag"),
                                enabled = index < zones.lastIndex,
                                onClick = {
                                    onHaptic(ClockHapticEvent.TICK)
                                    onZonesChanged(
                                        WorldClockOrderPolicy.move(
                                            zones = zones,
                                            index = index,
                                            offset = 1,
                                        )
                                    )
                                },
                            ) { Text("Move down") }
                            TextButton(
                                modifier = Modifier.testTag("world-clock-remove-$tag"),
                                onClick = {
                                    onHaptic(ClockHapticEvent.ACTION)
                                    onZonesChanged(WorldClockOrderPolicy.remove(zones, zone))
                                },
                            ) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add city") },
            text = {
                LazyColumn {
                    items(AvailableCities) { city ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = city.zoneId !in zones && city.zoneId != ZoneId.systemDefault().id,
                            onClick = {
                                onHaptic(ClockHapticEvent.ACTION)
                                onZonesChanged(WorldClockOrderPolicy.add(zones, city.zoneId))
                                showAdd = false
                            },
                        ) {
                            Text(city.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHaptic(ClockHapticEvent.ACTION)
                        showAdd = false
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun WorldClockCard(
    title: String,
    zoneId: String,
    instant: Instant,
    use24Hour: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        WorldClockCardContent(
            modifier = Modifier.padding(16.dp),
            title = title,
            zoneId = zoneId,
            instant = instant,
            use24Hour = use24Hour,
        )
    }
}

@Composable
private fun WorldClockCardContent(
    title: String,
    zoneId: String,
    instant: Instant,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.of(zoneId)
    val zoned = ZonedDateTime.ofInstant(instant, zone)
    val format = DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm:ss" else "h:mm:ss a")
    val offset = zoned.offset.id.let { if (it == "Z") "+00:00" else it }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                "UTC$offset • $zoneId",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(zoned.format(format), style = MaterialTheme.typography.headlineSmall)
    }
}
