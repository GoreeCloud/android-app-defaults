package com.goreecloud.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.launcher.core.launcher.LauncherAppLockCredentialType
import com.goreecloud.launcher.core.launcher.LauncherAppLockState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherAppLockUiRuntimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unconfiguredSettingsExplainLauncherOnlyBoundaryAndSetupChoices() {
        composeRule.setContent {
            MaterialTheme {
                LauncherAppLockSettingsContent(
                    state = LauncherAppLockState(),
                    apps = emptyList(),
                    onSetUp = {},
                    onChangeCredential = {},
                    onDisable = {},
                    onUnlockApp = {},
                )
            }
        }

        composeRule.onNodeWithText("Launcher-only protection")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "It does not lock the app across Android",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Set up PIN").assertIsDisplayed()
        composeRule.onNodeWithText("Set up pattern").assertIsDisplayed()
    }

    @Test
    fun setupDialogKeepsScopeWarningVisibleBeforeCredentialCreation() {
        composeRule.setContent {
            MaterialTheme {
                LauncherAppLockSetupDialog(
                    initialType = LauncherAppLockCredentialType.PIN,
                    onConfigure = { _, _ -> },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Set up App Lock").assertIsDisplayed()
        composeRule.onNodeWithText("Launcher-only protection").assertIsDisplayed()
        composeRule.onNodeWithText("Use 4–6 digits.").assertIsDisplayed()
        composeRule.onNodeWithText(
            "other launchers, notifications",
            substring = true,
        ).assertIsDisplayed()
    }
}
