package com.goreecloud.clock.alarm

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goreecloud.clock.R
import com.goreecloud.clock.system.NotificationChannels

object AlarmNotificationFactory {
    fun build(
        context: Context,
        alarm: Alarm,
        channelId: String = NotificationChannels.ALARM_PLAYBACK_CHANNEL_ID,
    ): Notification {
        val alertIntent = Intent(context, AlarmAlertActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            AlarmReceiver.notificationId(alarm.id),
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmReceiver.notificationId(alarm.id) xor 0x11000000,
            Intent(context, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_DISMISS)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmReceiver.notificationId(alarm.id) xor 0x22000000,
            Intent(context, AlarmActionReceiver::class.java)
                .setAction(AlarmActionReceiver.ACTION_SNOOZE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_clock_app)
            .setContentTitle(alarm.label.ifBlank { "Alarm" })
            .setContentText("Alarm is ringing")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_clock_app, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_clock_app, "Dismiss", dismissPendingIntent)
            .build()
    }

    fun postFallback(context: Context, alarm: Alarm) {
        NotificationChannels.ensure(context)
        val notification = build(context, alarm, NotificationChannels.ALARM_FALLBACK_CHANNEL_ID)
        val canPost =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

        if (canPost) {
            try {
                NotificationManagerCompat.from(context)
                    .notify(AlarmReceiver.notificationId(alarm.id), notification)
                return
            } catch (_: SecurityException) {
                // Fall through to the full-screen activity attempt.
            }
        }

        runCatching {
            context.startActivity(
                Intent(context, AlarmAlertActivity::class.java)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
    }
}
