package com.goreecloud.clock.alarm

import java.time.ZonedDateTime

data class UpcomingAlarm(
    val alarm: Alarm,
    val trigger: ZonedDateTime,
)

object UpcomingAlarmPolicy {
    fun next(
        alarms: List<Alarm>,
        now: ZonedDateTime,
    ): UpcomingAlarm? =
        alarms.asSequence()
            .mapNotNull { alarm ->
                AlarmScheduleCalculator.nextTrigger(alarm, now)?.let { trigger ->
                    UpcomingAlarm(alarm = alarm, trigger = trigger)
                }
            }
            .minWithOrNull(
                compareBy<UpcomingAlarm> { it.trigger.toInstant() }
                    .thenBy { it.alarm.id },
            )
}
