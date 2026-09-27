package com.goreecloud.clock.widget

import com.goreecloud.clock.alarm.Alarm
import com.goreecloud.clock.alarm.AlarmScheduleCalculator
import com.goreecloud.clock.timer.TimerEntry
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class ScheduledAlarm(
    val alarm: Alarm,
    val trigger: ZonedDateTime,
)

object NextAlarmSelector {
    fun select(
        alarms: List<Alarm>,
        now: ZonedDateTime,
    ): ScheduledAlarm? = alarms
        .asSequence()
        .filter { it.enabled }
        .mapNotNull { alarm ->
            AlarmScheduleCalculator.nextTrigger(alarm, now)
                ?.let { ScheduledAlarm(alarm, it) }
        }
        .minByOrNull { it.trigger.toInstant() }

    fun dayLabel(
        trigger: ZonedDateTime,
        now: ZonedDateTime,
    ): String = when (ChronoUnit.DAYS.between(now.toLocalDate(), trigger.toLocalDate())) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> trigger.dayOfWeek.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}

object TimerWidgetSelector {
    fun select(
        timers: List<TimerEntry>,
        nowEpochMillis: Long,
        elapsedRealtime: Long,
    ): TimerEntry? = timers
        .asSequence()
        .filter { it.running }
        .minByOrNull { it.remainingAt(nowEpochMillis, elapsedRealtime) }
}
