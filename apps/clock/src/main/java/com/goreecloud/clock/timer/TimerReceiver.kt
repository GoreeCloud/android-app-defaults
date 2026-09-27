package com.goreecloud.clock.timer

import android.Manifest
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

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(TimerScheduler.EXTRA_TIMER_ID, -1L)
        if (id < 0L) return

        val app = context.applicationContext as ClockApplication
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
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(id), notification)
        } catch (_: SecurityException) {
            return
        }
    }

    companion object {
        fun notificationId(id: Long): Int =
            ((id xor (id ushr 32)).toInt() and 0x0FFFFFFF) or 0x20000000
    }
}
