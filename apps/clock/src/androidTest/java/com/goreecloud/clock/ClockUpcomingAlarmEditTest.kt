package com.goreecloud.clock

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.goreecloud.clock.alarm.AlarmScheduler
import com.goreecloud.clock.alarm.AlarmStore
import com.goreecloud.clock.ui.screens.AlarmScreen
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ClockUpcomingAlarmEditTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var alarmStore: AlarmStore

    @Before
    fun resetAlarmState() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("clock_alarms", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        alarmStore = AlarmStore(context)
    }

    @After
    fun clearAlarmState() {
        context.getSharedPreferences("clock_alarms", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun nextAlarmCardOpensTheExistingAlarmEditor() {
        val time = LocalTime.now().plusMinutes(10)
        alarmStore.add(
            hour = time.hour,
            minute = time.minute,
            label = "Morning review",
            repeatDays = DayOfWeek.entries.toSet(),
            vibrate = true,
            snoozeMinutes = 10,
        )

        composeRule.setContent {
            MaterialTheme {
                AlarmScreen(
                    use24Hour = true,
                    alarmStore = alarmStore,
                    scheduler = AlarmScheduler(context),
                    exactAlarmAccess = true,
                    notificationAccess = true,
                    onRequestExactAlarmAccess = {},
                    onRequestNotificationAccess = {},
                    showHint = false,
                    onDismissHint = {},
                    onHaptic = {},
                )
            }
        }

        composeRule.onNodeWithTag("next-alarm-card")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Edit alarm").assertIsDisplayed()
        composeRule.onNodeWithText("Morning review").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Edit alarm").assertDoesNotExist()
    }
}
