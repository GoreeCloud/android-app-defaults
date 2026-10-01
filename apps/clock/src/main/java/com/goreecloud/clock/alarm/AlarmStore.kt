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
        soundKey: String = AlarmSound.DEFAULT,
        gradualVolumeSeconds: Int = 0,
    ): Alarm {
        val current = mutableAlarms.value
        val nextId = maxOf(System.currentTimeMillis(), (current.maxOfOrNull { it.id } ?: 0L) + 1L)
        val alarm = Alarm(
            id = nextId,
            hour = hour,
            minute = minute,
            label = label.trim(),
            enabled = true,
            repeatDays = repeatDays,
            vibrate = vibrate,
            snoozeMinutes = snoozeMinutes.coerceIn(1, 60),
            soundKey = soundKey,
            gradualVolumeSeconds = gradualVolumeSeconds.coerceIn(0, 60),
        )
        persist(current + alarm)
        return alarm
    }

    @Synchronized
    fun upsert(alarm: Alarm) = persist(mutableAlarms.value.filterNot { it.id == alarm.id } + alarm)

    @Synchronized
    fun setEnabled(id: Long, enabled: Boolean): Alarm? {
        val existing = get(id) ?: return null
        return existing.copy(enabled = enabled).also(::upsert)
    }

    @Synchronized
    fun delete(id: Long) = persist(mutableAlarms.value.filterNot { it.id == id })

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
        val days = alarm.repeatDays.sortedBy { it.value }.joinToString(",") { it.value.toString() }
        return listOf(
            alarm.id,
            alarm.hour,
            alarm.minute,
            alarm.enabled,
            days,
            alarm.vibrate,
            alarm.snoozeMinutes,
            alarm.gradualVolumeSeconds,
            encodeText(alarm.soundKey),
            encodeText(alarm.label),
        ).joinToString("|")
    }

    private fun decode(raw: String): Alarm? = runCatching {
        val parts = raw.split("|")
        val days = parts[4]
            .split(",")
            .filter { it.isNotBlank() }
            .map { DayOfWeek.of(it.toInt()) }
            .toSet()

        when (parts.size) {
            8 -> Alarm(
                id = parts[0].toLong(),
                hour = parts[1].toInt(),
                minute = parts[2].toInt(),
                enabled = parts[3].toBooleanStrict(),
                repeatDays = days,
                vibrate = parts[5].toBooleanStrict(),
                snoozeMinutes = parts[6].toInt(),
                soundKey = AlarmSound.DEFAULT,
                gradualVolumeSeconds = 0,
                label = decodeText(parts[7]),
            )
            10 -> Alarm(
                id = parts[0].toLong(),
                hour = parts[1].toInt(),
                minute = parts[2].toInt(),
                enabled = parts[3].toBooleanStrict(),
                repeatDays = days,
                vibrate = parts[5].toBooleanStrict(),
                snoozeMinutes = parts[6].toInt(),
                gradualVolumeSeconds = parts[7].toInt().coerceIn(0, 60),
                soundKey = decodeText(parts[8]),
                label = decodeText(parts[9]),
            )
            else -> null
        }
    }.getOrNull()

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private companion object {
        const val KEY_ALARMS = "alarms"
    }
}
