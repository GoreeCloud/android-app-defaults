package com.goreecloud.clock.alarm

import java.time.DayOfWeek
import java.time.ZonedDateTime

object AlarmSound {
    const val DEFAULT = "default"
    const val SILENT = "silent"
}

enum class AlarmRepeatPreset {
    ONE_TIME,
    EVERY_DAY,
    WEEKDAYS,
    WEEKENDS,
}

object AlarmRepeatPresetPolicy {
    private val everyDay = DayOfWeek.entries.toSet()
    private val weekdays = DayOfWeek.entries.filter { it.value <= DayOfWeek.FRIDAY.value }.toSet()
    private val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun daysFor(preset: AlarmRepeatPreset): Set<DayOfWeek> = when (preset) {
        AlarmRepeatPreset.ONE_TIME -> emptySet()
        AlarmRepeatPreset.EVERY_DAY -> everyDay
        AlarmRepeatPreset.WEEKDAYS -> weekdays
        AlarmRepeatPreset.WEEKENDS -> weekends
    }

    fun matchingPreset(days: Set<DayOfWeek>): AlarmRepeatPreset? =
        AlarmRepeatPreset.entries.firstOrNull { days == daysFor(it) }
}

data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val soundKey: String = AlarmSound.DEFAULT,
    val gradualVolumeSeconds: Int = 0,
)

object AlarmVolumeRamp {
    private const val MINIMUM_AUDIBLE_VOLUME = 0.15f

    fun volumeAt(elapsedMillis: Long, durationSeconds: Int): Float {
        if (durationSeconds <= 0) return 1f
        val durationMillis = durationSeconds * 1_000L
        val progress = (elapsedMillis.coerceAtLeast(0L).toFloat() / durationMillis)
            .coerceIn(0f, 1f)
        return MINIMUM_AUDIBLE_VOLUME + (1f - MINIMUM_AUDIBLE_VOLUME) * progress
    }
}

object AlarmScheduleCalculator {
    fun nextTrigger(
        alarm: Alarm,
        now: ZonedDateTime,
    ): ZonedDateTime? {
        if (!alarm.enabled) return null
        require(alarm.hour in 0..23)
        require(alarm.minute in 0..59)

        val maxDays = if (alarm.repeatDays.isEmpty()) 1 else 7
        for (delta in 0..maxDays) {
            val date = now.toLocalDate().plusDays(delta.toLong())
            if (alarm.repeatDays.isNotEmpty() && date.dayOfWeek !in alarm.repeatDays) {
                continue
            }

            val candidate = date
                .atTime(alarm.hour, alarm.minute)
                .atZone(now.zone)
                .withSecond(0)
                .withNano(0)

            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}
