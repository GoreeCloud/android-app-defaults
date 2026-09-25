package com.goreecloud.since.visual

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.since.MainActivity
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SinceMainActivitySystemBarsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivitySystemBarsFollowExplicitLightAndDarkThemes() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Light").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("Since").assertIsDisplayed()
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(
                activity.window,
                activity.window.decorView,
            )
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
        }
        capture("main-activity-light")

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Dark").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("Since").assertIsDisplayed()
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(
                activity.window,
                activity.window.decorView,
            )
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
        }
        capture("main-activity-dark")
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
                "Failed to encode visual evidence screenshot: $name"
            }
        }
        bitmap.recycle()
        check(outputFile.isFile && outputFile.length() > 0L) {
            "Visual evidence screenshot was not written: $name"
        }
    }
}
