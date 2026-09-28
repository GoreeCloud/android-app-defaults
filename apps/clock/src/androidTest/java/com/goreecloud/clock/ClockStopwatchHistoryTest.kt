package com.goreecloud.clock

import android.os.SystemClock
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockStopwatchHistoryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetStopwatch() {
        val app = composeRule.activity.application as ClockApplication
        app.preferencesStore.completeOnboarding()
        app.stopwatchStore.reset()
        app.stopwatchStore.clearHistory()
        composeRule.waitForIdle()
    }

    @Test
    fun resetArchivesSessionAndHistoryCanBeCleared() {
        val app = composeRule.activity.application as ClockApplication

        app.stopwatchStore.start()
        SystemClock.sleep(40L)
        app.stopwatchStore.pause()
        app.stopwatchStore.reset()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Stopwatch").performClick()
        composeRule.onNodeWithText("Recent results").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recent results").assertDoesNotExist()
    }
}
