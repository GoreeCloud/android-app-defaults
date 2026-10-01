package com.goreecloud.clock.alarm

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmScheduleCalculatorTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun oneTimeAlarmUsesTodayWhenStillAhead() {
        val now = ZonedDateTime.of(2026, 9, 27, 8, 0, 0, 0, zone)
        val alarm = Alarm(1, 9, 30, "", enabled = true)

        val next = AlarmScheduleCalculator.nextTrigger(alarm, now)

        assertEquals(
            ZonedDateTime.of(2026, 9, 27, 9, 30, 0, 0, zone),
            next,
        )
    }

    @Test
    fun oneTimeAlarmMovesToTomorrowWhenTimePassed() {
        val now = ZonedDateTime.of(2026, 9, 27, 10, 0, 0, 0, zone)
        val alarm = Alarm(1, 9, 30, "", enabled = true)

        val next = AlarmScheduleCalculator.nextTrigger(alarm, now)

        assertEquals(
            ZonedDateTime.of(2026, 9, 28, 9, 30, 0, 0, zone),
            next,
        )
    }

    @Test
    fun repeatingAlarmSkipsToNextSelectedDay() {
        val now = ZonedDateTime.of(2026, 9, 27, 10, 0, 0, 0, zone)
        val alarm = Alarm(
            id = 1,
            hour = 7,
            minute = 15,
            label = "",
            enabled = true,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )

        val next = AlarmScheduleCalculator.nextTrigger(alarm, now)

        assertEquals(DayOfWeek.MONDAY, next?.dayOfWeek)
        assertEquals(7, next?.hour)
        assertEquals(15, next?.minute)
    }

    @Test
    fun disabledAlarmHasNoTrigger() {
        val now = ZonedDateTime.of(2026, 9, 27, 10, 0, 0, 0, zone)
        val alarm = Alarm(1, 11, 0, "", enabled = false)

        assertNull(AlarmScheduleCalculator.nextTrigger(alarm, now))
    }
}
