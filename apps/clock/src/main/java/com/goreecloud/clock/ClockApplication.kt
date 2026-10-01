package com.goreecloud.clock

import android.app.Application
import com.goreecloud.clock.alarm.AlarmScheduler
import com.goreecloud.clock.alarm.AlarmStore
import com.goreecloud.clock.data.ClockPreferencesStore
import com.goreecloud.clock.stopwatch.StopwatchStore
import com.goreecloud.clock.system.NotificationChannels
import com.goreecloud.clock.timer.TimerScheduler
import com.goreecloud.clock.timer.TimerStore

class ClockApplication : Application() {
    lateinit var preferencesStore: ClockPreferencesStore
        private set
    lateinit var alarmStore: AlarmStore
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set
    lateinit var timerStore: TimerStore
        private set
    lateinit var timerScheduler: TimerScheduler
        private set
    lateinit var stopwatchStore: StopwatchStore
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesStore = ClockPreferencesStore(this)
        alarmStore = AlarmStore(this)
        alarmScheduler = AlarmScheduler(this)
        timerStore = TimerStore(this)
        timerScheduler = TimerScheduler(this)
        stopwatchStore = StopwatchStore(this)
        NotificationChannels.ensure(this)
    }
}
