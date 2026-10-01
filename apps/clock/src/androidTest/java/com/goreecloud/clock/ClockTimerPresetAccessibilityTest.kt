package com.goreecloud.clock

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.goreecloud.clock.timer.TimerScheduler
import com.goreecloud.clock.timer.TimerStore
import com.goreecloud.clock.ui.screens.TimerScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ClockTimerPresetAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var timerStore: TimerStore
    private lateinit var timerScheduler: TimerScheduler

    @Before
    fun resetTimerState() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("clock_timers", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        timerStore = TimerStore(context)
        timerScheduler = TimerScheduler(context)
    }

    @Test
    fun presetsAndDurationFieldsRemainReachableAtTwoHundredPercentText() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = 1f,
                    fontScale = 2f,
                ),
            ) {
                MaterialTheme {
                    TimerScreen(
                        timerStore = timerStore,
                        scheduler = timerScheduler,
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
        }

        composeRule.onNodeWithText("Add timer").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Quick presets").assertIsDisplayed()
        composeRule.onNodeWithTag("timer-preset-1h")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("timer-duration-hours")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("1")
        composeRule.onNodeWithTag("timer-duration-minutes")
            .assertIsDisplayed()
            .assertTextContains("0")
        composeRule.onNodeWithTag("timer-duration-seconds")
            .assertIsDisplayed()
            .assertTextContains("0")
        composeRule.onNodeWithText("Add").assertIsDisplayed()
    }

    @Test
    fun presetsRemainUsableWithForcedRtlLayout() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                MaterialTheme {
                    TimerScreen(
                        timerStore = timerStore,
                        scheduler = timerScheduler,
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
        }

        composeRule.onNodeWithText("Add timer").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("timer-preset-10m")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("timer-duration-minutes")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("10")
        composeRule.onNodeWithText("Add").assertIsDisplayed()
    }
}
