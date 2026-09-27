package com.goreecloud.clock.timer

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerStore(context: Context) {
    private val prefs = context.getSharedPreferences("clock_timers", Context.MODE_PRIVATE)
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
            endAtEpochMillis = 0L,
        )
        persist(current + item)
        return item
    }

    @Synchronized
    fun start(id: Long, now: Long = System.currentTimeMillis()): TimerEntry? {
        val item = get(id) ?: return null
        val remaining = item.remainingAt(now).let {
            if (it <= 0L) item.durationMillis else it
        }
        val updated = item.copy(
            remainingMillis = remaining,
            running = true,
            endAtEpochMillis = now + remaining,
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun pause(id: Long, now: Long = System.currentTimeMillis()): TimerEntry? {
        val item = get(id) ?: return null
        val updated = item.copy(
            remainingMillis = item.remainingAt(now),
            running = false,
            endAtEpochMillis = 0L,
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
            endAtEpochMillis = 0L,
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
            endAtEpochMillis = 0L,
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
    }

    private fun readAll(): List<TimerEntry> = prefs.getString(KEY_TIMERS, "")
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull(::decode)
        .toList()

    private fun encode(item: TimerEntry): String {
        val label = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(item.label.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            item.id,
            item.durationMillis,
            item.remainingMillis,
            item.running,
            item.endAtEpochMillis,
            label,
        ).joinToString("|")
    }

    private fun decode(raw: String): TimerEntry? = runCatching {
        val parts = raw.split("|")
        if (parts.size != 6) return@runCatching null
        TimerEntry(
            id = parts[0].toLong(),
            durationMillis = parts[1].toLong(),
            remainingMillis = parts[2].toLong(),
            running = parts[3].toBooleanStrict(),
            endAtEpochMillis = parts[4].toLong(),
            label = String(
                Base64.getUrlDecoder().decode(parts[5]),
                StandardCharsets.UTF_8,
            ),
        )
    }.getOrNull()

    private companion object {
        const val KEY_TIMERS = "timers"
    }
}
