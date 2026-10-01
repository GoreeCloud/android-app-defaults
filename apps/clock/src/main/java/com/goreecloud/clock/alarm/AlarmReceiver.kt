package com.goreecloud.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goreecloud.clock.ClockApplication

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (id < 0L) return

        val app = context.applicationContext as ClockApplication
        val alarm = app.alarmStore.get(id) ?: return
        val snoozeFire = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE_FIRE, false)

        if (!snoozeFire) {
            if (alarm.repeatDays.isEmpty()) {
                app.alarmStore.upsert(alarm.copy(enabled = false))
            } else {
                app.alarmScheduler.schedule(alarm)
            }
        }

        if (!AlarmPlaybackService.start(context, alarm.id)) {
            AlarmNotificationFactory.postFallback(context, alarm)
        }
    }

    companion object {
        fun notificationId(id: Long): Int =
            ((id xor (id ushr 32)).toInt() and 0x0FFFFFFF) or 0x10000000
    }
}
