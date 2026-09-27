package com.goreecloud.clock.alarm

import android.content.Context
import com.goreecloud.clock.widget.ClockWidgetUpdater
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlarmStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("clock_alarms", Context.MODE_PRIVATE)
    private val mutableAlarms = MutableStateFlow(readAll())

    val alarms = mutableAlarms.asStateFlow()

    @Synchronized
    fun add(
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: Set<DayOfWeek>,
        vibrate: Boolean,
        snoozeMinutes: Int,
    ): Alarm {
        val current = mutableAlarms.value
        val nextId = maxOf(
            System.currentTimeMillis(),
            (current.maxOfOrNull { it.id } ?: 0L) + 1L,
        )
        val alarm = Alarm(
            id = nextId,
            hour = hour,
            minute = minute,
            label = label.trim(),
            enabled = true,
            repeatDays = repeatDays,
            vibrate = vibrate,
            snoozeMinutes = snoozeMinutes.coerceIn(1, 60),
        )
        persist(current + alarm)
        return alarm
    }

    @Synchronized
    fun upsert(alarm: Alarm) {
        val updated = mutableAlarms.value
            .filterNot { it.id == alarm.id } + alarm
        persist(updated)
    }

    @Synchronized
    fun setEnabled(id: Long, enabled: Boolean): Alarm? {
        val existing = get(id) ?: return null
        val updated = existing.copy(enabled = enabled)
        upsert(updated)
        return updated
    }

    @Synchronized
    fun delete(id: Long) {
        persist(mutableAlarms.value.filterNot { it.id == id })
    }

    fun get(id: Long): Alarm? = mutableAlarms.value.firstOrNull { it.id == id }

    private fun persist(items: List<Alarm>) {
        val sorted = items.sortedWith(compareBy<Alarm> { it.hour }.thenBy { it.minute }.thenBy { it.id })
        prefs.edit().putString(KEY_ALARMS, sorted.joinToString("\n", transform = ::encode)).apply()
        mutableAlarms.value = sorted
        ClockWidgetUpdater.updateAlarmWidgets(appContext)
    }

    private fun readAll(): List<Alarm> = prefs.getString(KEY_ALARMS, "")
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull(::decode)
        .toList()

    private fun encode(alarm: Alarm): String {
        val days = alarm.repeatDays
            .sortedBy { it.value }
            .joinToString(",") { it.value.toString() }
        val encodedLabel = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(alarm.label.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            alarm.id,
            alarm.hour,
            alarm.minute,
            alarm.enabled,
            days,
            alarm.vibrate,
            alarm.snoozeMinutes,
            encodedLabel,
        ).joinToString("|")
    }

    private fun decode(raw: String): Alarm? = runCatching {
        val parts = raw.split("|")
        if (parts.size != 8) return@runCatching null
        val days = parts[4]
            .split(",")
            .filter { it.isNotBlank() }
            .map { DayOfWeek.of(it.toInt()) }
            .toSet()
        val label = String(
            Base64.getUrlDecoder().decode(parts[7]),
            StandardCharsets.UTF_8,
        )
        Alarm(
            id = parts[0].toLong(),
            hour = parts[1].toInt(),
            minute = parts[2].toInt(),
            enabled = parts[3].toBooleanStrict(),
            repeatDays = days,
            vibrate = parts[5].toBooleanStrict(),
            snoozeMinutes = parts[6].toInt(),
            label = label,
        )
    }.getOrNull()

    private companion object {
        const val KEY_ALARMS = "alarms"
    }
}
