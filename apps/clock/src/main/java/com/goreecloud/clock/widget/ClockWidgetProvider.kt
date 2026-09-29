package com.goreecloud.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.MainActivity
import com.goreecloud.clock.R

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        private const val REQUEST_OPEN_CLOCK = 4101

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle? = null,
        ) {
            val app = context.applicationContext as? ClockApplication
            val use24Hour = app?.preferencesStore?.state?.value?.use24Hour
                ?: DateFormat.is24HourFormat(context)
            val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
            val widgetOptions = options ?: manager.getAppWidgetOptions(appWidgetId)
            val presentation = WidgetSizePolicy.presentation(
                minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
            )
            val compact = presentation == WidgetPresentation.COMPACT
            val showSecondaryDetail = WidgetDetailsPolicy.showSecondaryDetail(
                presentation = presentation,
                preferenceEnabled = app?.preferencesStore?.state?.value?.showWidgetDetails ?: true,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_clock).apply {
                setViewVisibility(
                    R.id.widget_clock_date,
                    if (showSecondaryDetail) View.VISIBLE else View.GONE,
                )
                setTextViewTextSize(
                    R.id.widget_clock_time,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (compact) 30f else 38f,
                )
                setCharSequence(R.id.widget_clock_time, "setFormat12Hour", timePattern)
                setCharSequence(R.id.widget_clock_time, "setFormat24Hour", timePattern)
                setCharSequence(R.id.widget_clock_date, "setFormat12Hour", "EEE, MMM d")
                setCharSequence(R.id.widget_clock_date, "setFormat24Hour", "EEE, MMM d")
                setOnClickPendingIntent(
                    R.id.widget_clock_root,
                    ClockWidgetUpdater.openDestination(
                        context,
                        MainActivity.DESTINATION_CLOCK,
                        REQUEST_OPEN_CLOCK,
                    ),
                )
                setContentDescription(
                    R.id.widget_clock_root,
                    context.getString(R.string.widget_clock_content_description),
                )
            }
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
