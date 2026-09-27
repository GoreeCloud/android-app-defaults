package com.goreecloud.clock.widget

import com.goreecloud.clock.alarm.Alarm
import com.goreecloud.clock.timer.TimerEntry
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetModelsTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun nextAlarmSelectorChoosesEarliestEnabledTrigger() {
        val now = ZonedDateTime.of(2026, 9, 27, 8, 0, 0, 0, zone)
        val alarms = listOf(
            Alarm(1, 9, 30, "Later", enabled = true),
            Alarm(2, 8, 45, "Sooner", enabled = true),
            Alarm(3, 8, 15, "Disabled", enabled = false),
        )

        val selected = NextAlarmSelector.select(alarms, now)

        assertEquals(2L, selected?.alarm?.id)
        assertEquals("Today", selected?.let { NextAlarmSelector.dayLabel(it.trigger, now) })
    }

    @Test
    fun nextAlarmSelectorRespectsRepeatDays() {
        val now = ZonedDateTime.of(2026, 9, 27, 10, 0, 0, 0, zone)
        val alarms = listOf(
            Alarm(
                id = 1,
                hour = 7,
                minute = 0,
                label = "Work",
                enabled = true,
                repeatDays = setOf(DayOfWeek.MONDAY),
            ),
        )

        val selected = NextAlarmSelector.select(alarms, now)

        assertEquals(DayOfWeek.MONDAY, selected?.trigger?.dayOfWeek)
        assertEquals("Tomorrow", selected?.let { NextAlarmSelector.dayLabel(it.trigger, now) })
    }

    @Test
    fun timerWidgetSelectorUsesSoonestRunningTimer() {
        val timers = listOf(
            TimerEntry(1, "Paused", 60_000L, 20_000L, false, 0L, 0L),
            TimerEntry(2, "Long", 60_000L, 60_000L, true, 10_000L, 100_000L),
            TimerEntry(3, "Short", 30_000L, 30_000L, true, 20_000L, 100_000L),
        )

        val selected = TimerWidgetSelector.select(
            timers,
            nowEpochMillis = 120_000L,
            elapsedRealtime = 35_000L,
        )

        assertEquals(3L, selected?.id)
    }

    @Test
    fun timerWidgetSelectorReturnsNullWithoutRunningTimer() {
        val timers = listOf(
            TimerEntry(1, "Paused", 60_000L, 20_000L, false, 0L, 0L),
        )

        assertNull(
            TimerWidgetSelector.select(
                timers,
                nowEpochMillis = 120_000L,
                elapsedRealtime = 35_000L,
            ),
        )
    }
}
