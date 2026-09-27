package com.goreecloud.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.MainActivity
import com.goreecloud.clock.R

class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    companion object {
        private const val REQUEST_OPEN_TIMER = 4301

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val app = context.applicationContext as ClockApplication
            val nowWall = System.currentTimeMillis()
            val nowElapsed = SystemClock.elapsedRealtime()
            val timer = TimerWidgetSelector.select(
                app.timerStore.timers.value,
                nowEpochMillis = nowWall,
                elapsedRealtime = nowElapsed,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_timer)

            if (timer == null) {
                views.setViewVisibility(R.id.widget_timer_countdown, View.GONE)
                views.setViewVisibility(R.id.widget_timer_empty, View.VISIBLE)
                views.setTextViewText(R.id.widget_timer_label, context.getString(R.string.widget_timer_title))
                views.setTextViewText(R.id.widget_timer_status, "")
            } else {
                val remaining = timer.remainingAt(nowWall, nowElapsed)
                val base = nowElapsed + remaining
                views.setViewVisibility(R.id.widget_timer_countdown, View.VISIBLE)
                views.setViewVisibility(R.id.widget_timer_empty, View.GONE)
                views.setChronometer(R.id.widget_timer_countdown, base, null, true)
                views.setChronometerCountDown(R.id.widget_timer_countdown, true)
                views.setTextViewText(
                    R.id.widget_timer_label,
                    timer.label.ifBlank { context.getString(R.string.widget_timer_title) },
                )
                views.setTextViewText(
                    R.id.widget_timer_status,
                    if (app.timerScheduler.canScheduleExactAlarms()) {
                        context.getString(R.string.widget_timer_running)
                    } else {
                        context.getString(R.string.widget_timer_exact_access_needed)
                    },
                )
            }

            views.setOnClickPendingIntent(
                R.id.widget_timer_root,
                ClockWidgetUpdater.openDestination(
                    context,
                    MainActivity.DESTINATION_TIMER,
                    REQUEST_OPEN_TIMER,
                ),
            )
            views.setContentDescription(
                R.id.widget_timer_root,
                context.getString(R.string.widget_timer_content_description),
            )
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
