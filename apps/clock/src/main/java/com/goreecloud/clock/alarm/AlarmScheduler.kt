package com.goreecloud.clock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.MainActivity
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(alarm: Alarm): Boolean {
        cancel(alarm.id)
        if (!alarm.enabled) return true
        if (!canScheduleExactAlarms()) return false

        val trigger = AlarmScheduleCalculator.nextTrigger(
            alarm = alarm,
            now = ZonedDateTime.now(),
        ) ?: return false

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(
                trigger.toInstant().toEpochMilli(),
                showAlarmPendingIntent(),
            ),
            operationPendingIntent(alarm.id, snooze = false),
        )
        return true
    }

    fun scheduleSnooze(alarm: Alarm): Boolean {
        if (!canScheduleExactAlarms()) return false
        val triggerAt = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showAlarmPendingIntent()),
            operationPendingIntent(alarm.id, snooze = true),
        )
        return true
    }

    fun cancel(id: Long) {
        alarmManager.cancel(operationPendingIntent(id, snooze = false))
        alarmManager.cancel(operationPendingIntent(id, snooze = true))
    }

    fun rescheduleAll() {
        if (!canScheduleExactAlarms()) return
        val app = context.applicationContext as ClockApplication
        app.alarmStore.alarms.value
            .filter { it.enabled }
            .forEach(::schedule)
    }

    private fun showAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_ALARMS)
        return PendingIntent.getActivity(
            context,
            REQUEST_SHOW_ALARMS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun operationPendingIntent(id: Long, snooze: Boolean): PendingIntent {
        val alarm = (context.applicationContext as ClockApplication).alarmStore.get(id)
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(if (snooze) ACTION_SNOOZE_FIRE else ACTION_ALARM_FIRE)
            .putExtra(EXTRA_ALARM_ID, id)
            .putExtra(EXTRA_SNOOZE_FIRE, snooze)
        if (alarm != null) {
            intent.putExtra(EXTRA_LABEL, alarm.label)
            intent.putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id, snooze),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(id: Long, snooze: Boolean): Int {
        val folded = (id xor (id ushr 32)).toInt()
        return if (snooze) folded xor SNOOZE_MASK else folded
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_SNOOZE_FIRE = "snooze_fire"
        const val ACTION_ALARM_FIRE = "com.goreecloud.clock.ALARM_FIRE"
        const val ACTION_SNOOZE_FIRE = "com.goreecloud.clock.ALARM_SNOOZE_FIRE"

        private const val REQUEST_SHOW_ALARMS = 1001
        private const val SNOOZE_MASK = 0x40000000
    }
}
