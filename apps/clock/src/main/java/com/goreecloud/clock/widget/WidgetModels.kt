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


enum class WidgetPresentation {
    COMPACT,
    REGULAR,
}

object WidgetDetailPolicy {
    fun showSecondaryDetail(
        presentation: WidgetPresentation,
        enabled: Boolean,
    ): Boolean = enabled && presentation == WidgetPresentation.REGULAR
}

object WidgetSizePolicy {
    private const val COMPACT_WIDTH_DP = 160
    private const val COMPACT_HEIGHT_DP = 90

    fun presentation(
        minWidthDp: Int,
        minHeightDp: Int,
    ): WidgetPresentation {
        val widthIsCompact = minWidthDp > 0 && minWidthDp < COMPACT_WIDTH_DP
        val heightIsCompact = minHeightDp > 0 && minHeightDp < COMPACT_HEIGHT_DP
        return if (widthIsCompact || heightIsCompact) {
            WidgetPresentation.COMPACT
        } else {
            WidgetPresentation.REGULAR
        }
    }
}
