package com.goreecloud.clock

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.clock.data.ThemePreference
import java.io.File
import java.io.FileOutputStream
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockOnboardingVisualEvidenceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetOnboarding() {
        val store = (composeRule.activity.application as ClockApplication).preferencesStore
        store.setTheme(ThemePreference.LIGHT)
        store.setHintsEnabled(true)
        store.resetOnboardingGuidance()
        composeRule.waitForIdle()
    }

    @Test
    fun captureOnboardingScenes() {
        composeRule.onNodeWithText("Welcome to GoreeCloud Clock").assertIsDisplayed()
        capture("onboarding-welcome")

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Guidance & permissions").assertIsDisplayed()
        capture("onboarding-guidance")
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputDirectory = File(
            instrumentation.targetContext.filesDir,
            "visual-evidence",
        ).apply { check(mkdirs() || isDirectory) }
        val outputFile = File(outputDirectory, "$name.png")
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        FileOutputStream(outputFile).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        bitmap.recycle()
        check(outputFile.isFile && outputFile.length() > 0L)
    }
}
