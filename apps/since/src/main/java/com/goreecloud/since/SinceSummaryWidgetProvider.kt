package com.goreecloud.since

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Privacy-safe Since summary widget.
 *
 * The launcher surface receives only an aggregate count of active trackers. Tracker titles, notes,
 * start times, streak history, reset reasons, goals, and archived state are intentionally excluded
 * from RemoteViews.
 */
class SinceSummaryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SinceSummaryWidgetProvider::class.java),
            )
            updateAsync(context, manager, ids)
            return
        }
        super.onReceive(context, intent)
    }

    private fun updateAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        if (ids.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SinceApplication
                val count = app.database.trackerDao().activeTrackerCount()
                val views = RemoteViews(context.packageName, R.layout.widget_since_summary).apply {
                    setTextViewText(R.id.widget_since_count, count.toString())
                    setTextViewText(
                        R.id.widget_since_label,
                        context.resources.getQuantityString(
                            R.plurals.widget_active_trackers,
                            count,
                            count,
                        ),
                    )
                    setOnClickPendingIntent(
                        R.id.widget_since_root,
                        openSinceIntent(context),
                    )
                }
                ids.forEach { id -> manager.updateAppWidget(id, views) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun openSinceIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val ACTION_REFRESH = "com.goreecloud.since.action.REFRESH_SUMMARY_WIDGET"

        fun requestRefresh(context: Context) {
            context.sendBroadcast(
                Intent(context, SinceSummaryWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                },
            )
        }
    }
}
