package com.goreecloud.clock.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goreecloud.clock.ClockApplication

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
        if (!supported) return

        val app = context.applicationContext as ClockApplication
        app.alarmScheduler.rescheduleAll()
        app.timerScheduler.rescheduleAll()
    }
}
