package com.goreecloud.clock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.clock.data.OnboardingStep
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockOnboardingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startFreshOnboarding() {
        val store = (composeRule.activity.application as ClockApplication).preferencesStore
        store.setHintsEnabled(true)
        store.resetDismissedHints()
        store.resetOnboardingGuidance()
        composeRule.waitForIdle()
    }

    @Test
    fun onboardingResumesCompletesAndCanBeReplayed() {
        val store = (composeRule.activity.application as ClockApplication).preferencesStore

        composeRule.onNodeWithText("Welcome to GoreeCloud Clock").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Time & display").assertIsDisplayed()
        check(store.state.value.onboardingStep == OnboardingStep.TIME_DISPLAY)

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Time & display").assertIsDisplayed()

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Guidance & permissions").assertIsDisplayed()
        composeRule.onNodeWithText("Permissions stay contextual").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Ready to go").assertIsDisplayed()
        composeRule.onNodeWithText("Start using Clock").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Digital").assertIsDisplayed()
        check(store.state.value.onboardingCompleted)

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Contextual hints").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Replay onboarding").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome to GoreeCloud Clock").assertIsDisplayed()
        check(store.state.value.onboardingCompleted)
        check(store.state.value.onboardingReplay)

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome to GoreeCloud Clock").assertIsDisplayed()
        check(store.state.value.onboardingCompleted)

        composeRule.onNodeWithText("Close replay").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Digital").assertIsDisplayed()
        check(store.state.value.onboardingCompleted)
        check(!store.state.value.onboardingReplay)
    }
}
