package com.goreecloud.clock.alarm

import java.time.DayOfWeek
import java.time.ZonedDateTime

data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
)

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

            if (candidate.isAfter(now)) {
                return candidate
            }
        }

        return null
    }
}
