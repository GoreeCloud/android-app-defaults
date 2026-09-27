package com.goreecloud.clock.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchStateTest {
    @Test
    fun runningStopwatchUsesMonotonicClockWhenAvailable() {
        val state = StopwatchState(
            running = true,
            accumulatedMillis = 5_000L,
            startedElapsedRealtime = 10_000L,
            startedWallMillis = 100_000L,
        )

        assertEquals(
            8_000L,
            state.elapsedAt(elapsedRealtime = 13_000L, wallMillis = 500_000L),
        )
    }

    @Test
    fun runningStopwatchFallsBackToWallClockAfterMonotonicReset() {
        val state = StopwatchState(
            running = true,
            accumulatedMillis = 5_000L,
            startedElapsedRealtime = 80_000L,
            startedWallMillis = 100_000L,
        )

        assertEquals(
            15_000L,
            state.elapsedAt(elapsedRealtime = 5_000L, wallMillis = 110_000L),
        )
    }
}
