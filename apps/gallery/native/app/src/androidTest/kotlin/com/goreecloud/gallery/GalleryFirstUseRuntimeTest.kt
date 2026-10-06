package com.goreecloud.gallery

import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryFirstUseRuntimeTest {
    @Test
    fun firstUseSetupPersistsAndRemainsReplayable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = context.getSharedPreferences(
            GallerySetupPreferences.PREFERENCES_NAME,
            android.content.Context.MODE_PRIVATE,
        )
        assertTrue(
            raw.edit()
                // Explicitly request the incomplete-setup state. Removing this key would invoke
                // production upgrade detection, and instrumentation installs may legitimately
                // report lastUpdateTime > firstInstallTime even on an otherwise clean emulator.
                .putBoolean(GallerySetupPreferences.ONBOARDING_COMPLETE_KEY, false)
                .remove(GallerySetupPreferences.ONBOARDING_STEP_KEY)
                .remove(GallerySetupPreferences.CONTEXTUAL_HINTS_ENABLED_KEY)
                .remove(GallerySetupPreferences.DISMISSED_CONTEXTUAL_HINTS_KEY)
                .commit(),
        )

        ActivityScenario.launch(GalleryActivity::class.java).use { scenario ->
            onView(withText("Set up Gallery")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withContentDescription("Continue Gallery setup"))
                .inRoot(isDialog())
                .check(matches(isAssignableFrom(ImageView::class.java)))
                .perform(directClick())
            waitForDialogText("You control media access")

            scenario.recreate()
            waitForDialogText("You control media access")
            onView(withText("Set up Gallery")).inRoot(isDialog()).check(matches(isDisplayed()))

            onView(withContentDescription("Continue Gallery setup")).inRoot(isDialog()).perform(directClick())
            waitForDialogText("Choose your guidance")
            onView(withContentDescription(containsString("Contextual hints."))).inRoot(isDialog()).perform(directClick())
            onView(withContentDescription("Finish Gallery setup")).inRoot(isDialog()).perform(directClick())

            val preferences = GallerySetupPreferences(context)
            assertFalse(preferences.shouldShowSetup())
            assertFalse(preferences.contextualHintsEnabled())
            assertFalse(
                preferences.isContextualHintDismissed(GallerySetupPreferences.HINT_PHOTOS),
            )
            preferences.dismissContextualHint(GallerySetupPreferences.HINT_PHOTOS)
            assertTrue(
                GallerySetupPreferences(context)
                    .isContextualHintDismissed(GallerySetupPreferences.HINT_PHOTOS),
            )
            preferences.resetDismissedContextualHints()
            assertFalse(
                GallerySetupPreferences(context)
                    .isContextualHintDismissed(GallerySetupPreferences.HINT_PHOTOS),
            )

            scenario.recreate()
            onView(withText("Set up Gallery")).check(doesNotExist())
            onView(withContentDescription("Settings")).perform(directClick())
            onView(withContentDescription(containsString("Replay setup.")))
                .perform(scrollTo(), directClick())
            waitForDialogText("Review Gallery setup")
            onView(withContentDescription("Return to Gallery")).inRoot(isDialog()).perform(directClick())
            onView(withText("Review Gallery setup")).check(doesNotExist())
        }
    }

    private fun directClick(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()

        override fun getDescription(): String = "invoke View.performClick() for rendered Gallery control"

        override fun perform(uiController: UiController, view: View) {
            assertTrue(
                "Expected rendered Gallery control to handle performClick()",
                view.performClick(),
            )
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun waitForDialogText(
        expected: String,
        timeoutMillis: Long = 3_000L,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var lastFailure: Throwable? = null

        while (SystemClock.uptimeMillis() < deadline) {
            try {
                onView(withText(expected))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                return
            } catch (failure: AssertionError) {
                lastFailure = failure
            } catch (failure: RuntimeException) {
                lastFailure = failure
            }

            instrumentation.waitForIdleSync()
            SystemClock.sleep(100)
        }

        throw AssertionError(
            "Timed out waiting for Gallery setup dialog text: $expected",
            lastFailure,
        )
    }
}
