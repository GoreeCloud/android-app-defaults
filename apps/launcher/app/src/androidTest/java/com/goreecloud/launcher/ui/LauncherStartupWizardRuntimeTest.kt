package com.goreecloud.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherStartupWizardRuntimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wizardOffersHomeModesAndReturnsSelectedConfiguration() {
        var completed: LauncherStartupConfiguration? = null

        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = false,
                    initialHomeAppMode = LauncherHomeAppMode.NONE,
                    initialHomeColumns = 5,
                    initialHomeRows = 6,
                    initialShowHomeLabels = true,
                    initialUniversalSearchHomeMode =
                        LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                    initialAddNewAppsToHome = false,
                    initialShowHints = true,
                    onRequestHomeRole = {},
                    onFinish = { completed = it },
                )
            }
        }

        composeRule.onNodeWithText("Welcome to GoreeCloud Launcher")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        composeRule.onNodeWithText("Build your Home")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("No automatic apps")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("10 most recent apps")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("10 most used apps")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        composeRule.onNodeWithText("Search and gestures")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Finish setup").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { completed != null }
        val result = completed
        assertNotNull(result)
        assertEquals(LauncherHomeAppMode.MOST_USED, result?.homeAppMode)
        assertEquals(5, result?.homeColumns)
        assertEquals(6, result?.homeRows)
        assertEquals(LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY, result?.universalSearchHomeMode)
    }
    @Test
    fun wizardKeepsAdvancedDetailsBehindLearnMore() {
        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = true,
                    initialHomeAppMode = LauncherHomeAppMode.NONE,
                    initialHomeColumns = 5,
                    initialHomeRows = 6,
                    initialShowHomeLabels = true,
                    initialUniversalSearchHomeMode =
                        LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                    initialAddNewAppsToHome = false,
                    initialShowHints = true,
                    initialStep = 2,
                    onRequestHomeRole = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithText("Search and gestures")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Apps")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Widgets")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Folders")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Private Search")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Learn more")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Exact placement")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Connected Search")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun homeHintCoversExactPlacementLivePageSwitchAndCurrentFolderAddPath() {
        composeRule.setContent {
            MaterialTheme {
                LauncherHomeHintCard(onDismiss = {})
            }
        }

        composeRule.onNodeWithText(
            "Long-press an app in Apps to drag it to Home/Dock or Pin in Apps; use Pinned first or the ★ filter to keep favorites easy to reach.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Home apps: keep holding at a left or right page edge briefly to switch pages, then release over the exact target cell.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Widgets: long-press and drag to a free Home cell or adjacent page edge; movable Universal Search uses the same Home-grid behavior.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Folders: long-press a Home folder to move it to a free cell or adjacent page edge; use Add apps at the end of the grid and swipe larger opened folders between pages.",
        ).assertIsDisplayed()
    }

    @Test
    fun wizardResumesAtPersistedStepAndReportsProgress() {
        var reportedStep = -1

        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = true,
                    initialHomeAppMode = LauncherHomeAppMode.RECENT,
                    initialHomeColumns = 5,
                    initialHomeRows = 6,
                    initialShowHomeLabels = true,
                    initialUniversalSearchHomeMode =
                        LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                    initialAddNewAppsToHome = false,
                    initialShowHints = true,
                    initialStep = 1,
                    onStepChange = { reportedStep = it },
                    onRequestHomeRole = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithText("Build your Home")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        assertEquals(2, reportedStep)
        composeRule.onNodeWithText("Search and gestures")
            .performScrollTo()
            .assertIsDisplayed()
    }

}
