package com.goreecloud.clock.alarm

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UpcomingAlarmPresentationPolicyTest {
    private val zone = ZoneId.of("America/Chicago")

    @Test
    fun relativeSummaryUsesInstantDurationAcrossLocalClockChanges() {
        val now = ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0, zone)
        val trigger = now.plusHours(3)

        assertEquals("In 3 h", UpcomingAlarmPresentationPolicy.relativeSummary(now, trigger))
    }

    @Test
    fun relativeSummaryKeepsNearAndMultiDayCopyCompact() {
        val now = ZonedDateTime.of(2026, 9, 28, 12, 0, 0, 0, zone)

        assertEquals(
            "In less than 1 min",
            UpcomingAlarmPresentationPolicy.relativeSummary(now, now.plusSeconds(30)),
        )
        assertEquals(
            "In 2 h 15 min",
            UpcomingAlarmPresentationPolicy.relativeSummary(now, now.plusMinutes(135)),
        )
        assertEquals(
            "In 2 d 3 h",
            UpcomingAlarmPresentationPolicy.relativeSummary(now, now.plusHours(51)),
        )
        assertEquals(
            "Due now",
            UpcomingAlarmPresentationPolicy.relativeSummary(now, now.minusMinutes(1)),
        )
    }
}
