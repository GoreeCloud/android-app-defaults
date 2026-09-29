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
    fun presetCatalogExposesBoundedUsefulDurations() {
        assertEquals(
            listOf("1m", "5m", "10m", "15m", "30m", "1h"),
            TimerPresetCatalog.defaults.map { it.key },
        )
        assertEquals(
            listOf(60_000L, 300_000L, 600_000L, 900_000L, 1_800_000L, 3_600_000L),
            TimerPresetCatalog.defaults.map { it.durationMillis },
        )
    }

    @Test
    fun presetDurationConvertsToEditorFieldsWithoutLosingSeconds() {
        assertEquals(
            Triple("1", "2", "3"),
            TimerPresetCatalog.fieldsFor(3_723_000L),
        )
    }

    @Test
    fun formatterSupportsHoursAndHundredths() {
        assertEquals("1:02:03", DurationFormatter.format(3_723_000L))
        assertEquals("01:02.34", DurationFormatter.format(62_340L, showHundredths = true))
    }
}
