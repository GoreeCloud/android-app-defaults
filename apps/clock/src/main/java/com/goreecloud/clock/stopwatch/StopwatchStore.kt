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

data class StopwatchResult(
    val id: Long,
    val finishedAtWallMillis: Long,
    val elapsedMillis: Long,
    val laps: List<Long>,
)

object StopwatchResultFactory {
    fun fromState(
        state: StopwatchState,
        id: Long,
        finishedAtWallMillis: Long,
        elapsedRealtime: Long,
        wallMillis: Long,
    ): StopwatchResult? {
        val elapsed = state.elapsedAt(elapsedRealtime, wallMillis)
        if (elapsed <= 0L) return null
        return StopwatchResult(id, finishedAtWallMillis, elapsed, state.laps)
    }
}

class StopwatchStore(context: Context) {
    private val prefs = context.getSharedPreferences("clock_stopwatch", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())
    private val mutableHistory = MutableStateFlow(readHistory())

    val state = mutableState.asStateFlow()
    val history = mutableHistory.asStateFlow()

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
        val current = mutableState.value
        val finishedAt = System.currentTimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()
        val nextId = maxOf(
            finishedAt,
            (mutableHistory.value.maxOfOrNull { it.id } ?: 0L) + 1L,
        )
        StopwatchResultFactory.fromState(
            state = current,
            id = nextId,
            finishedAtWallMillis = finishedAt,
            elapsedRealtime = elapsedRealtime,
            wallMillis = finishedAt,
        )?.let(::archive)

        return StopwatchState().also(::persist)
    }

    @Synchronized
    fun lap(): StopwatchState {
        val current = mutableState.value
        if (!current.running) return current
        val updated = current.copy(laps = current.laps + current.elapsedAt())
        persist(updated)
        return updated
    }

    @Synchronized
    fun deleteResult(id: Long) {
        persistHistory(mutableHistory.value.filterNot { it.id == id })
    }

    @Synchronized
    fun clearHistory() {
        persistHistory(emptyList())
    }

    private fun archive(result: StopwatchResult) {
        persistHistory(
            (listOf(result) + mutableHistory.value)
                .distinctBy { it.id }
                .sortedByDescending { it.finishedAtWallMillis }
                .take(MAX_HISTORY_RESULTS),
        )
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

    private fun persistHistory(results: List<StopwatchResult>) {
        val bounded = results
            .sortedByDescending { it.finishedAtWallMillis }
            .take(MAX_HISTORY_RESULTS)
        prefs.edit()
            .putString(KEY_HISTORY, bounded.joinToString("\n", transform = ::encodeResult))
            .apply()
        mutableHistory.value = bounded
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

    private fun readHistory(): List<StopwatchResult> = prefs.getString(KEY_HISTORY, "")
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull(::decodeResult)
        .sortedByDescending { it.finishedAtWallMillis }
        .take(MAX_HISTORY_RESULTS)
        .toList()

    private fun encodeResult(result: StopwatchResult): String = listOf(
        result.id,
        result.finishedAtWallMillis,
        result.elapsedMillis,
        result.laps.joinToString(","),
    ).joinToString("|")

    private fun decodeResult(raw: String): StopwatchResult? = runCatching {
        val parts = raw.split("|", limit = 4)
        if (parts.size != 4) return@runCatching null
        StopwatchResult(
            id = parts[0].toLong(),
            finishedAtWallMillis = parts[1].toLong(),
            elapsedMillis = parts[2].toLong(),
            laps = parts[3].split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { it.toLongOrNull() },
        )
    }.getOrNull()

    private companion object {
        const val KEY_RUNNING = "running"
        const val KEY_ACCUMULATED = "accumulated"
        const val KEY_STARTED_ELAPSED = "started_elapsed"
        const val KEY_STARTED_WALL = "started_wall"
        const val KEY_LAPS = "laps"
        const val KEY_HISTORY = "history_v1"
        const val MAX_HISTORY_RESULTS = 20
    }
}
