package com.goreecloud.clock.alarm

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpcomingAlarmPolicyTest {
    private val zone = ZoneId.of("America/Chicago")
    private val now = ZonedDateTime.of(2026, 9, 28, 6, 30, 0, 0, zone)

    @Test
    fun choosesEarliestEnabledOccurrenceAcrossAlarms() {
        val alarms = listOf(
            Alarm(id = 1, hour = 8, minute = 0, label = "Later"),
            Alarm(id = 2, hour = 7, minute = 15, label = "Soon"),
            Alarm(id = 3, hour = 6, minute = 45, label = "Disabled", enabled = false),
        )

        val upcoming = UpcomingAlarmPolicy.next(alarms, now)

        assertEquals(2L, upcoming?.alarm?.id)
        assertEquals(7, upcoming?.trigger?.hour)
        assertEquals(15, upcoming?.trigger?.minute)
    }

    @Test
    fun repeatingAlarmCanBeatTomorrowOneTimeAlarm() {
        val alarms = listOf(
            Alarm(id = 1, hour = 6, minute = 0, label = "Tomorrow"),
            Alarm(
                id = 2,
                hour = 21,
                minute = 0,
                label = "Tonight",
                repeatDays = setOf(DayOfWeek.MONDAY),
            ),
        )

        val upcoming = UpcomingAlarmPolicy.next(alarms, now)

        assertEquals(2L, upcoming?.alarm?.id)
        assertEquals(DayOfWeek.MONDAY, upcoming?.trigger?.dayOfWeek)
    }

    @Test
    fun returnsNullWhenNoAlarmIsEnabled() {
        assertNull(
            UpcomingAlarmPolicy.next(
                listOf(Alarm(id = 1, hour = 8, minute = 0, label = "", enabled = false)),
                now,
            )
        )
    }
}
