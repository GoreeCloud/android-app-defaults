package com.goreecloud.clock.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.R
import com.goreecloud.clock.system.NotificationChannels

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

        NotificationChannels.ensure(context)
        val alertIntent = Intent(context, AlarmAlertActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId(alarm.id),
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_DISMISS)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(AlarmActionReceiver.ACTION_SNOOZE)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId(alarm.id) xor 0x11000000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId(alarm.id) xor 0x22000000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = alarm.label.ifBlank { "Alarm" }
        val notification = NotificationCompat.Builder(context, NotificationChannels.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock_app)
            .setContentTitle(title)
            .setContentText("Alarm is ringing")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_clock_app, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_clock_app, "Dismiss", dismissPendingIntent)
            .apply {
                if (alarm.vibrate) {
                    setVibrate(longArrayOf(0L, 500L, 500L, 500L))
                }
            }
            .build()

        if (canPostNotifications(context)) {
            NotificationManagerCompat.from(context).notify(notificationId(alarm.id), notification)
        } else {
            runCatching { context.startActivity(alertIntent) }
        }
    }

    companion object {
        fun notificationId(id: Long): Int =
            ((id xor (id ushr 32)).toInt() and 0x0FFFFFFF) or 0x10000000

        private fun canPostNotifications(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
}
