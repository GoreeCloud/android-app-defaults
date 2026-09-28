package com.goreecloud.clock.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

object NotificationChannels {
    const val ALARM_PLAYBACK_CHANNEL_ID = "clock_alarm_playback_v2"
    const val ALARM_FALLBACK_CHANNEL_ID = "clock_alarm_fallback_v1"
    const val TIMER_CHANNEL_ID = "clock_timers"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val playbackChannel = NotificationChannel(
            ALARM_PLAYBACK_CHANNEL_ID,
            "Active alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Visible controls while a GoreeCloud Clock alarm is ringing"
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null)
        }

        val fallbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val fallbackChannel = NotificationChannel(
            ALARM_FALLBACK_CHANNEL_ID,
            "Alarm fallback alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "System fallback alert if active alarm playback cannot start"
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), fallbackAttributes)
        }

        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            "Timers",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "GoreeCloud Clock timer completion alerts"
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(playbackChannel, fallbackChannel, timerChannel))
    }
}
