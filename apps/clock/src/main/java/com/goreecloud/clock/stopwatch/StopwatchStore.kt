package com.goreecloud.clock.stopwatch

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StopwatchState(
    val running: Boolean = false,
    val accumulatedMillis: Long = 0L,
    val startedElapsedRealtime: Long = 0L,
    val startedWallMillis: Long = 0L,
    val laps: List<Long> = emptyList(),
) {
    fun elapsedAt(
        elapsedRealtime: Long = SystemClock.elapsedRealtime(),
        wallMillis: Long = System.currentTimeMillis(),
    ): Long {
        if (!running) return accumulatedMillis.coerceAtLeast(0L)
        val delta = if (elapsedRealtime >= startedElapsedRealtime) {
            elapsedRealtime - startedElapsedRealtime
        } else {
            (wallMillis - startedWallMillis).coerceAtLeast(0L)
        }
        return (accumulatedMillis + delta).coerceAtLeast(0L)
    }
}

class StopwatchStore(context: Context) {
    private val prefs = context.getSharedPreferences("clock_stopwatch", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())
    val state = mutableState.asStateFlow()

    @Synchronized
    fun start(): StopwatchState {
        val current = mutableState.value
        if (current.running) return current
        val updated = current.copy(
            running = true,
            startedElapsedRealtime = SystemClock.elapsedRealtime(),
            startedWallMillis = System.currentTimeMillis(),
        )
        persist(updated)
        return updated
    }

    @Synchronized
    fun pause(): StopwatchState {
        val current = mutableState.value
        if (!current.running) return current
        val updated = current.copy(
            running = false,
            accumulatedMillis = current.elapsedAt(),
            startedElapsedRealtime = 0L,
            startedWallMillis = 0L,
        )
        persist(updated)
        return updated
    }

    @Synchronized
    fun reset(): StopwatchState {
        val updated = StopwatchState()
        persist(updated)
        return updated
    }

    @Synchronized
    fun lap(): StopwatchState {
        val current = mutableState.value
        if (!current.running) return current
        val updated = current.copy(laps = current.laps + current.elapsedAt())
        persist(updated)
        return updated
    }

    private fun persist(value: StopwatchState) {
        prefs.edit()
            .putBoolean(KEY_RUNNING, value.running)
            .putLong(KEY_ACCUMULATED, value.accumulatedMillis)
            .putLong(KEY_STARTED_ELAPSED, value.startedElapsedRealtime)
            .putLong(KEY_STARTED_WALL, value.startedWallMillis)
            .putString(KEY_LAPS, value.laps.joinToString(","))
            .apply()
        mutableState.value = value
    }

    private fun read(): StopwatchState = StopwatchState(
        running = prefs.getBoolean(KEY_RUNNING, false),
        accumulatedMillis = prefs.getLong(KEY_ACCUMULATED, 0L),
        startedElapsedRealtime = prefs.getLong(KEY_STARTED_ELAPSED, 0L),
        startedWallMillis = prefs.getLong(KEY_STARTED_WALL, 0L),
        laps = prefs.getString(KEY_LAPS, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toLongOrNull() },
    )

    private companion object {
        const val KEY_RUNNING = "running"
        const val KEY_ACCUMULATED = "accumulated"
        const val KEY_STARTED_ELAPSED = "started_elapsed"
        const val KEY_STARTED_WALL = "started_wall"
        const val KEY_LAPS = "laps"
    }
}
