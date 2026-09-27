package com.goreecloud.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.goreecloud.clock.ClockApplication

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (id < 0L) return

        val app = context.applicationContext as ClockApplication
        if (intent.action == ACTION_SNOOZE) {
            app.alarmStore.get(id)?.let(app.alarmScheduler::scheduleSnooze)
        }
        NotificationManagerCompat.from(context).cancel(AlarmReceiver.notificationId(id))
    }

    companion object {
        const val ACTION_SNOOZE = "com.goreecloud.clock.SNOOZE_ALARM"
        const val ACTION_DISMISS = "com.goreecloud.clock.DISMISS_ALARM"
    }
}
