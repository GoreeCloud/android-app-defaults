package com.goreecloud.clock.alarm

import java.time.Duration
import java.time.ZonedDateTime

object UpcomingAlarmPresentationPolicy {
    fun relativeSummary(
        now: ZonedDateTime,
        trigger: ZonedDateTime,
    ): String {
        val duration = Duration.between(now.toInstant(), trigger.toInstant())
        if (duration.isNegative || duration.isZero) return "Due now"

        val totalMinutes = duration.toMinutes()
        if (totalMinutes == 0L) return "In less than 1 min"

        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes % (24L * 60L)) / 60L
        val minutes = totalMinutes % 60L

        val parts = buildList {
            if (days > 0) add("$days d")
            if (hours > 0) add("$hours h")
            if (minutes > 0 && days == 0L) add("$minutes min")
        }
        return "In " + parts.joinToString(" ")
    }
}
