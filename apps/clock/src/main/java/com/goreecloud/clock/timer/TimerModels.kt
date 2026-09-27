package com.goreecloud.clock.timer

import android.os.SystemClock

data class TimerEntry(
    val id: Long,
    val label: String,
    val durationMillis: Long,
    val remainingMillis: Long,
    val running: Boolean,
    val startedElapsedRealtime: Long,
    val startedWallMillis: Long,
) {
    fun remainingAt(
        nowEpochMillis: Long = System.currentTimeMillis(),
        elapsedRealtime: Long = SystemClock.elapsedRealtime(),
    ): Long {
        if (!running) return remainingMillis.coerceAtLeast(0L)

        val elapsed = if (
            startedElapsedRealtime > 0L &&
            elapsedRealtime >= startedElapsedRealtime
        ) {
            elapsedRealtime - startedElapsedRealtime
        } else {
            (nowEpochMillis - startedWallMillis).coerceAtLeast(0L)
        }

        return (remainingMillis - elapsed).coerceAtLeast(0L)
    }
}

object DurationFormatter {
    fun format(millis: Long, showHundredths: Boolean = false): String {
        val safe = millis.coerceAtLeast(0L)
        val totalSeconds = safe / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        val base = if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
        if (!showHundredths) return base
        val hundredths = (safe % 1_000L) / 10L
        return "$base.%02d".format(hundredths)
    }
}
