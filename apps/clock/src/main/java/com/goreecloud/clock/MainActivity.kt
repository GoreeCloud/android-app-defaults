package com.goreecloud.clock

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goreecloud.clock.data.ThemePreference
import com.goreecloud.clock.ui.ClockApp
import com.goreecloud.clock.ui.theme.ClockTheme

class MainActivity : ComponentActivity() {
    private val exactAlarmAccess = mutableStateOf(true)
    private val notificationAccess = mutableStateOf(true)
    private val destinationRequest = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationAccess.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        destinationRequest.value = intent.getStringExtra(EXTRA_DESTINATION)
        refreshPermissionState()

        val app = application as ClockApplication
        setContent {
            val preferences by app.preferencesStore.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (preferences.theme) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            ClockTheme(darkTheme = darkTheme) {
                ClockApp(
                    preferences = preferences,
                    preferencesStore = app.preferencesStore,
                    alarmStore = app.alarmStore,
                    alarmScheduler = app.alarmScheduler,
                    timerStore = app.timerStore,
                    timerScheduler = app.timerScheduler,
                    stopwatchStore = app.stopwatchStore,
                    exactAlarmAccess = exactAlarmAccess.value,
                    notificationAccess = notificationAccess.value,
                    destinationRequest = destinationRequest.value,
                    onDestinationConsumed = { destinationRequest.value = null },
                    onRequestExactAlarmAccess = ::requestExactAlarmAccess,
                    onRequestNotificationAccess = ::requestNotificationAccess,
                    onPresentationModeChanged = ::setPresentationMode,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
        val app = application as ClockApplication
        if (exactAlarmAccess.value) {
            app.alarmScheduler.rescheduleAll()
            app.timerScheduler.rescheduleAll()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destinationRequest.value = intent.getStringExtra(EXTRA_DESTINATION)
    }

    private fun refreshPermissionState() {
        val alarmManager = getSystemService(AlarmManager::class.java)
        exactAlarmAccess.value =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        notificationAccess.value =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmAccess.value) return
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationAccess.value
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setPresentationMode(immersive: Boolean, keepScreenOn: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val DESTINATION_ALARMS = "alarms"
    }
}
