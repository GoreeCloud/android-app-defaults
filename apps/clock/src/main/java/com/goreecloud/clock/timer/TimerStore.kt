package com.goreecloud.clock.timer

import android.content.Context
import android.os.SystemClock
import com.goreecloud.clock.widget.ClockWidgetUpdater
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("clock_timers", Context.MODE_PRIVATE)
    private val mutableTimers = MutableStateFlow(readAll())
    val timers = mutableTimers.asStateFlow()

    @Synchronized
    fun add(label: String, durationMillis: Long): TimerEntry {
        require(durationMillis > 0L)
        val current = mutableTimers.value
        val id = maxOf(
            System.currentTimeMillis(),
            (current.maxOfOrNull { it.id } ?: 0L) + 1L,
        )
        val item = TimerEntry(
            id = id,
            label = label.trim(),
            durationMillis = durationMillis,
            remainingMillis = durationMillis,
            running = false,
            startedElapsedRealtime = 0L,
            startedWallMillis = 0L,
        )
        persist(current + item)
        return item
    }

    @Synchronized
    fun start(
        id: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
        elapsedRealtime: Long = SystemClock.elapsedRealtime(),
    ): TimerEntry? {
        val item = get(id) ?: return null
        val currentRemaining = item.remainingAt(nowEpochMillis, elapsedRealtime)
        val remaining = if (currentRemaining <= 0L) item.durationMillis else currentRemaining
        val updated = item.copy(
            remainingMillis = remaining,
            running = true,
            startedElapsedRealtime = elapsedRealtime,
            startedWallMillis = nowEpochMillis,
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun pause(
        id: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
        elapsedRealtime: Long = SystemClock.elapsedRealtime(),
    ): TimerEntry? {
        val item = get(id) ?: return null
        val updated = item.copy(
            remainingMillis = item.remainingAt(nowEpochMillis, elapsedRealtime),
            running = false,
            startedElapsedRealtime = 0L,
            startedWallMillis = 0L,
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun reset(id: Long): TimerEntry? {
        val item = get(id) ?: return null
        val updated = item.copy(
            remainingMillis = item.durationMillis,
            running = false,
            startedElapsedRealtime = 0L,
            startedWallMillis = 0L,
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun complete(id: Long): TimerEntry? {
        val item = get(id) ?: return null
        val updated = item.copy(
            remainingMillis = 0L,
            running = false,
            startedElapsedRealtime = 0L,
            startedWallMillis = 0L,
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun delete(id: Long) {
        persist(mutableTimers.value.filterNot { it.id == id })
    }

    fun get(id: Long): TimerEntry? = mutableTimers.value.firstOrNull { it.id == id }

    private fun upsert(item: TimerEntry) {
        persist(mutableTimers.value.filterNot { it.id == item.id } + item)
    }

    private fun persist(items: List<TimerEntry>) {
        val sorted = items.sortedBy { it.id }
        prefs.edit().putString(KEY_TIMERS, sorted.joinToString("\n", transform = ::encode)).apply()
        mutableTimers.value = sorted
        ClockWidgetUpdater.updateTimerWidgets(appContext)
    }

    private fun readAll(): List<TimerEntry> {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        return prefs.getString(KEY_TIMERS, "")
            .orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { decode(it, nowWall, nowElapsed) }
            .toList()
    }

    private fun encode(item: TimerEntry): String {
        val label = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(item.label.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            item.id,
            item.durationMillis,
            item.remainingMillis,
            item.running,
            item.startedElapsedRealtime,
            item.startedWallMillis,
            label,
        ).joinToString("|")
    }

    private fun decode(
        raw: String,
        nowWall: Long,
        nowElapsed: Long,
    ): TimerEntry? = runCatching {
        val parts = raw.split("|")
        when (parts.size) {
            7 -> TimerEntry(
                id = parts[0].toLong(),
                durationMillis = parts[1].toLong(),
                remainingMillis = parts[2].toLong(),
                running = parts[3].toBooleanStrict(),
                startedElapsedRealtime = parts[4].toLong(),
                startedWallMillis = parts[5].toLong(),
                label = decodeLabel(parts[6]),
            )
            6 -> {
                val duration = parts[1].toLong()
                val storedRemaining = parts[2].toLong()
                val wasRunning = parts[3].toBooleanStrict()
                val legacyEndAtWall = parts[4].toLong()
                val migratedRemaining = if (wasRunning) {
                    (legacyEndAtWall - nowWall).coerceIn(0L, duration)
                } else {
                    storedRemaining.coerceIn(0L, duration)
                }
                TimerEntry(
                    id = parts[0].toLong(),
                    durationMillis = duration,
                    remainingMillis = migratedRemaining,
                    running = wasRunning && migratedRemaining > 0L,
                    startedElapsedRealtime = if (wasRunning && migratedRemaining > 0L) nowElapsed else 0L,
                    startedWallMillis = if (wasRunning && migratedRemaining > 0L) nowWall else 0L,
                    label = decodeLabel(parts[5]),
                )
            }
            else -> null
        }
    }.getOrNull()

    private fun decodeLabel(raw: String): String = String(
        Base64.getUrlDecoder().decode(raw),
        StandardCharsets.UTF_8,
    )

    private companion object {
        const val KEY_TIMERS = "timers"
    }
}
