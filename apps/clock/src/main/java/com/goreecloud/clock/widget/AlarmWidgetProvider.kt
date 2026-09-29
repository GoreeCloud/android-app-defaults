package com.goreecloud.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.MainActivity
import com.goreecloud.clock.R
import com.goreecloud.clock.alarm.UpcomingAlarmPresentationPolicy
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmWidgetProvider : AppWidgetProvider() {
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
        private const val REQUEST_OPEN_ALARMS = 4201

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AlarmWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle? = null,
        ) {
            val app = context.applicationContext as ClockApplication
            val preferences = app.preferencesStore.state.value
            val now = ZonedDateTime.now()
            val next = NextAlarmSelector.select(app.alarmStore.alarms.value, now)
            val widgetOptions = options ?: manager.getAppWidgetOptions(appWidgetId)
            val presentation = WidgetSizePolicy.presentation(
                minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
            )
            val compact = presentation == WidgetPresentation.COMPACT
            val views = RemoteViews(context.packageName, R.layout.widget_alarm).apply {
                setViewVisibility(
                    R.id.widget_alarm_detail,
                    if (
                        WidgetDetailPolicy.showSecondaryDetail(
                            presentation = presentation,
                            enabled = preferences.showAlarmWidgetDetail,
                        )
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    },
                )
                setTextViewTextSize(
                    R.id.widget_alarm_time,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (compact) 24f else 30f,
                )
                setTextViewTextSize(
                    R.id.widget_alarm_label,
                    TypedValue.COMPLEX_UNIT_SP,
                    if (compact) 13f else 14f,
                )
            }

            if (next == null) {
                views.setTextViewText(R.id.widget_alarm_time, context.getString(R.string.widget_alarm_none))
                views.setTextViewText(R.id.widget_alarm_label, context.getString(R.string.widget_alarm_add_hint))
                views.setTextViewText(R.id.widget_alarm_detail, "")
            } else {
                val pattern = if (preferences.use24Hour) "HH:mm" else "h:mm a"
                val time = next.trigger.format(DateTimeFormatter.ofPattern(pattern))
                val label = next.alarm.label.ifBlank { context.getString(R.string.widget_alarm_default_label) }
                val day = NextAlarmSelector.dayLabel(next.trigger, now)
                val detail = if (app.alarmScheduler.canScheduleExactAlarms()) {
                    AlarmWidgetDetailPolicy.summary(
                        dayLabel = day,
                        repeatSummary = UpcomingAlarmPresentationPolicy.repeatSummary(
                            next.alarm.repeatDays,
                        ),
                    )
                } else {
                    context.getString(R.string.widget_alarm_exact_access_needed)
                }
                views.setTextViewText(R.id.widget_alarm_time, time)
                views.setTextViewText(R.id.widget_alarm_label, label)
                views.setTextViewText(R.id.widget_alarm_detail, detail)
            }

            views.setOnClickPendingIntent(
                R.id.widget_alarm_root,
                ClockWidgetUpdater.openDestination(
                    context,
                    MainActivity.DESTINATION_ALARMS,
                    REQUEST_OPEN_ALARMS,
                ),
            )
            views.setContentDescription(
                R.id.widget_alarm_root,
                context.getString(R.string.widget_alarm_content_description),
            )
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
