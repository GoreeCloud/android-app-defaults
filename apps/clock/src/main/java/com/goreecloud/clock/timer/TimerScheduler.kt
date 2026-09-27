package com.goreecloud.clock.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.goreecloud.clock.ClockApplication

class TimerScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(timer: TimerEntry): Boolean {
        cancel(timer.id)
        if (!timer.running) return true
        if (!canScheduleExactAlarms()) return false

        val remaining = timer.remainingAt(
            nowEpochMillis = System.currentTimeMillis(),
            elapsedRealtime = SystemClock.elapsedRealtime(),
        )
        val triggerAtElapsed = SystemClock.elapsedRealtime() + remaining.coerceAtLeast(250L)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAtElapsed,
            pendingIntent(timer.id),
        )
        return true
    }

    fun cancel(id: Long) {
        alarmManager.cancel(pendingIntent(id))
    }

    fun rescheduleAll() {
        if (!canScheduleExactAlarms()) return
        val app = context.applicationContext as ClockApplication
        app.timerStore.timers.value
            .filter { it.running }
            .forEach(::schedule)
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java)
            .setAction(ACTION_TIMER_COMPLETE)
            .putExtra(EXTRA_TIMER_ID, id)
        return PendingIntent.getBroadcast(
            context,
            (id xor (id ushr 32)).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_TIMER_COMPLETE = "com.goreecloud.clock.TIMER_COMPLETE"
        const val EXTRA_TIMER_ID = "timer_id"
    }
}
