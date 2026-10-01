package com.goreecloud.clock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.clock.data.ClockFacePreference
import com.goreecloud.clock.data.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        app.preferencesStore.setShowClockWidgetDate(true)
        app.preferencesStore.setShowAlarmWidgetDetail(true)
        app.preferencesStore.setShowTimerWidgetStatus(true)
        app.preferencesStore.setWorldZones(emptyList())
        composeRule.waitForIdle()
    }

    @Test
    fun worldClockReorderPersistsAcrossActivityRecreation() {
        val app = composeRule.activity.application as ClockApplication
        app.preferencesStore.setWorldZones(listOf("Europe/London", "Asia/Tokyo"))
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("World").performClick()
        composeRule.onNodeWithTag("world-clock-Europe_London").assertIsDisplayed()
        composeRule.onNodeWithTag("world-clock-Asia_Tokyo").assertIsDisplayed()
        composeRule.onNodeWithTag("world-clock-move-down-Europe_London").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("Asia/Tokyo", "Europe/London"),
                app.preferencesStore.state.value.worldZones,
            )
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("World").performClick()
        composeRule.onNodeWithTag("world-clock-Asia_Tokyo").assertIsDisplayed()
        composeRule.onNodeWithTag("world-clock-Europe_London").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(
                listOf("Asia/Tokyo", "Europe/London"),
                app.preferencesStore.state.value.worldZones,
            )
        }

        composeRule.onNodeWithTag("world-clock-remove-Asia_Tokyo").performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf("Europe/London"),
                app.preferencesStore.state.value.worldZones,
            )
        }
    }

    @Test
    fun widgetDetailPreferencesPersistAcrossActivityRecreation() {
        val app = composeRule.activity.application as ClockApplication

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithTag("settings-widget-clock-date")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("settings-widget-alarm-detail")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("settings-widget-timer-status")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertFalse(app.preferencesStore.state.value.showClockWidgetDate)
            assertFalse(app.preferencesStore.state.value.showAlarmWidgetDetail)
            assertFalse(app.preferencesStore.state.value.showTimerWidgetStatus)
        }

        composeRule.onNodeWithText("Done").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertFalse(app.preferencesStore.state.value.showClockWidgetDate)
            assertFalse(app.preferencesStore.state.value.showAlarmWidgetDetail)
            assertFalse(app.preferencesStore.state.value.showTimerWidgetStatus)
        }
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
        composeRule.onNodeWithText("Alarm sound:", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Choose alarm sound").assertIsDisplayed()
        composeRule.onNodeWithText("System default").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Gradual volume").performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithText("Haptic feedback").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Widgets").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Show date in Clock widget").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Show detail in Next alarm widget").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Show status in Timer widget").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Contextual hints").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Replay onboarding").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithContentDescription("Clock").performClick()
        composeRule.onNodeWithText("Digital").assertIsDisplayed()
    }
}
