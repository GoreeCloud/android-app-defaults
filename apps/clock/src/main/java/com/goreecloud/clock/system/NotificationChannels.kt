package com.goreecloud.clock.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

object NotificationChannels {
    const val ALARM_CHANNEL_ID = "clock_alarms"
    const val TIMER_CHANNEL_ID = "clock_timers"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "GoreeCloud Clock alarm alerts"
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(alarmSound, alarmAttributes)
        }

        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            "Timers",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "GoreeCloud Clock timer completion alerts"
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(alarmChannel, timerChannel))
    }
}
