package com.goreecloud.clock.timer

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
import com.goreecloud.clock.MainActivity
import com.goreecloud.clock.R
import com.goreecloud.clock.system.NotificationChannels

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(TimerScheduler.EXTRA_TIMER_ID, -1L)
        if (id < 0L) return

        val app = context.applicationContext as ClockApplication
        if (intent.action == ACTION_TIMER_RESTART) {
            val restarted = app.timerStore.start(id) ?: return
            TimerScheduler(context.applicationContext).schedule(restarted)
            NotificationManagerCompat.from(context).cancel(notificationId(id))
            return
        }

        val timer = app.timerStore.complete(id) ?: return
        NotificationChannels.ensure(context)

        val canPostNotifications =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        if (!canPostNotifications) return

        val title = timer.label.ifBlank { "Timer" }
        val notification = NotificationCompat.Builder(context, NotificationChannels.TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clock_app)
            .setContentTitle(title)
            .setContentText("Timer finished")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openTimerPendingIntent(context))
            .addAction(
                0,
                "Restart",
                restartPendingIntent(context, id),
            )
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(id), notification)
        } catch (_: SecurityException) {
            return
        }
    }

    companion object {
        const val ACTION_TIMER_RESTART = "com.goreecloud.clock.TIMER_RESTART"

        fun notificationId(id: Long): Int =
            ((id xor (id ushr 32)).toInt() and 0x0FFFFFFF) or 0x20000000

        private fun openTimerPendingIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0x2401,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_TIMER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun restartPendingIntent(context: Context, id: Long): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId(id) xor 0x01000000,
                Intent(context, TimerReceiver::class.java)
                    .setAction(ACTION_TIMER_RESTART)
                    .putExtra(TimerScheduler.EXTRA_TIMER_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
