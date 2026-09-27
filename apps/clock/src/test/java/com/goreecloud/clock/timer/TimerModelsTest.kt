package com.goreecloud.clock.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerModelsTest {
    @Test
    fun runningTimerUsesMonotonicElapsedTime() {
        val timer = TimerEntry(
            id = 1,
            label = "",
            durationMillis = 60_000L,
            remainingMillis = 60_000L,
            running = true,
            startedElapsedRealtime = 10_000L,
            startedWallMillis = 100_000L,
        )

        assertEquals(
            25_000L,
            timer.remainingAt(nowEpochMillis = 500_000L, elapsedRealtime = 45_000L),
        )
    }

    @Test
    fun wallClockChangeDoesNotShiftTimerWithinSameBoot() {
        val timer = TimerEntry(
            id = 1,
            label = "",
            durationMillis = 60_000L,
            remainingMillis = 60_000L,
            running = true,
            startedElapsedRealtime = 10_000L,
            startedWallMillis = 100_000L,
        )

        assertEquals(
            50_000L,
            timer.remainingAt(nowEpochMillis = 9_000_000L, elapsedRealtime = 20_000L),
        )
    }

    @Test
    fun runningTimerFallsBackToWallClockAfterMonotonicReset() {
        val timer = TimerEntry(
            id = 1,
            label = "",
            durationMillis = 60_000L,
            remainingMillis = 60_000L,
            running = true,
            startedElapsedRealtime = 80_000L,
            startedWallMillis = 100_000L,
        )

        assertEquals(
            50_000L,
            timer.remainingAt(nowEpochMillis = 110_000L, elapsedRealtime = 5_000L),
        )
    }

    @Test
    fun formatterSupportsHoursAndHundredths() {
        assertEquals("1:02:03", DurationFormatter.format(3_723_000L))
        assertEquals("01:02.34", DurationFormatter.format(62_340L, showHundredths = true))
    }
}
