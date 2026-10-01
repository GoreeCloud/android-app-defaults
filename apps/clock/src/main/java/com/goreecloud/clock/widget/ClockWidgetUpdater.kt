package com.goreecloud.clock.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.goreecloud.clock.MainActivity

object ClockWidgetUpdater {
    fun updateAll(context: Context) {
        updateClockWidgets(context)
        updateAlarmWidgets(context)
        updateTimerWidgets(context)
    }

    fun updateClockWidgets(context: Context) {
        ClockWidgetProvider.updateAll(context.applicationContext)
    }

    fun updateAlarmWidgets(context: Context) {
        AlarmWidgetProvider.updateAll(context.applicationContext)
    }

    fun updateTimerWidgets(context: Context) {
        TimerWidgetProvider.updateAll(context.applicationContext)
    }

    internal fun openDestination(
        context: Context,
        destination: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
