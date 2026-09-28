package com.goreecloud.clock

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.clock.data.ClockFacePreference
import com.goreecloud.clock.data.ThemePreference
import java.io.File
import java.io.FileOutputStream
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockVisualEvidenceTest {
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
    fun capturePrincipalClockFlow() {
        composeRule.onNodeWithText("Digital").assertIsDisplayed().performClick()
        capture("clock-digital")

        composeRule.onNodeWithText("Analog").performClick()
        composeRule.onNodeWithContentDescription("Full-screen clock").assertIsDisplayed()
        composeRule.onNodeWithText("Bedside mode").assertIsDisplayed()
        capture("clock-analog")

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithText("Add alarm").assertIsDisplayed()
        capture("alarms-empty")
        composeRule.onNodeWithText("Add alarm").performClick()
        composeRule.onNodeWithText("New alarm").assertIsDisplayed()
        capture("alarm-editor")
        composeRule.onNodeWithText("Alarm sound:", substring = true).performClick()
        composeRule.onNodeWithText("Choose alarm sound").assertIsDisplayed()
        capture("alarm-sound-picker")
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithContentDescription("Timer").performClick()
        composeRule.onNodeWithText("Add timer").assertIsDisplayed()
        capture("timer-empty")
        composeRule.onNodeWithText("Add timer").performClick()
        composeRule.onNodeWithText("New timer").assertIsDisplayed()
        capture("timer-editor")
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithContentDescription("Stopwatch").performClick()
        composeRule.onNodeWithText("Laps").assertIsDisplayed()
        capture("stopwatch")

        composeRule.onNodeWithContentDescription("World").performClick()
        composeRule.onNodeWithText("Local").assertIsDisplayed()
        capture("world-clock")
        composeRule.onNodeWithText("Add world clock").performClick()
        composeRule.onNodeWithText("Add city").assertIsDisplayed()
        capture("world-clock-picker")
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Clock settings").assertIsDisplayed()
        capture("settings")
        composeRule.onNodeWithText("Dark").performClick()
        composeRule.waitForIdle()
        capture("settings-dark")
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithContentDescription("Clock").performClick()
        composeRule.onNodeWithText("Digital").performClick()
        composeRule.onNodeWithText("Digital").assertIsDisplayed()
        capture("clock-digital-dark")
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputDirectory = File(
            instrumentation.targetContext.filesDir,
            "visual-evidence",
        ).apply {
            check(mkdirs() || isDirectory)
        }
        val outputFile = File(outputDirectory, "$name.png")
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "Android UI automation did not return a screenshot."
        }

        FileOutputStream(outputFile).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Failed to encode Clock visual evidence screenshot: $name"
            }
        }
        bitmap.recycle()
        check(outputFile.isFile && outputFile.length() > 0L) {
            "Clock visual evidence screenshot was not written: $name"
        }
    }
}
