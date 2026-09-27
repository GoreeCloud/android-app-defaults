package com.goreecloud.clock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.clock.data.ClockFacePreference
import com.goreecloud.clock.data.ThemePreference
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetPresentationPreferences() {
        val app = composeRule.activity.application as ClockApplication
        app.preferencesStore.setTheme(ThemePreference.LIGHT)
        app.preferencesStore.setClockFace(ClockFacePreference.DIGITAL)
        app.preferencesStore.completeOnboarding()
        app.preferencesStore.setHintsEnabled(true)
        app.preferencesStore.resetDismissedHints()
        composeRule.waitForIdle()
    }

    @Test
    fun principalClockSurfacesAndEditorsRemainReachable() {
        composeRule.onNodeWithText("Digital").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Analog").performClick()
        composeRule.onNodeWithContentDescription("Full-screen clock").assertIsDisplayed()
        composeRule.onNodeWithText("Bedside mode").assertIsDisplayed()
        composeRule.onNodeWithText("Digital").performClick()

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithText("Add alarm").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("New alarm").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithContentDescription("Timer").performClick()
        composeRule.onNodeWithText("Add timer").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("New timer").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithContentDescription("Stopwatch").performClick()
        composeRule.onNodeWithText("Laps").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("World").performClick()
        composeRule.onNodeWithText("Local").assertIsDisplayed()
        composeRule.onNodeWithText("Add world clock").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Add city").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Clock settings").assertIsDisplayed()
        composeRule.onNodeWithText("24-hour time").assertIsDisplayed()
        composeRule.onNodeWithText("Contextual hints").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Replay onboarding").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithContentDescription("Clock").performClick()
        composeRule.onNodeWithText("Digital").assertIsDisplayed()
    }
}
