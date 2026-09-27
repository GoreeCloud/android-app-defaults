package com.goreecloud.clock.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerModelsTest {
    @Test
    fun runningTimerUsesEndTime() {
        val timer = TimerEntry(
            id = 1,
            label = "",
            durationMillis = 60_000L,
            remainingMillis = 60_000L,
            running = true,
            endAtEpochMillis = 100_000L,
        )

        assertEquals(25_000L, timer.remainingAt(75_000L))
        assertEquals(0L, timer.remainingAt(120_000L))
    }

    @Test
    fun formatterSupportsHoursAndHundredths() {
        assertEquals("1:02:03", DurationFormatter.format(3_723_000L))
        assertEquals("01:02.34", DurationFormatter.format(62_340L, showHundredths = true))
    }
}
