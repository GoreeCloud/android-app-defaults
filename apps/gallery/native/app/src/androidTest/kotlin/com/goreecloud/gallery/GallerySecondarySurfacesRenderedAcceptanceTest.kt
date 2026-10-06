package com.goreecloud.gallery

import android.graphics.Rect
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GallerySecondarySurfacesRenderedAcceptanceTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(RecycleBinActivity::class.java)

    @Test
    fun trashPersistentControlsUseGlazeAndRemainTouchSized() {
        listOf(
            "Refresh Trash",
        ).forEach { description ->
            onView(withContentDescription(description))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .check(matches(hasMinimumTouchSizeDp(48f)))
                .check(matches(hasIconOnlyGlyph()))
        }
    }

    @Test
    fun trashSurfaceExposesFivePrimaryDestinations() {
        listOf(
            "Photos",
            "Albums",
            "Videos",
            "Trash, selected",
            "Settings",
        ).forEach { description ->
            onView(withContentDescription(description))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .check(matches(hasMinimumTouchSizeDp(48f)))
        }
    }

    @Test
    fun trashNavigationUsesTheSameCenteredIconsOnlyGeometry() {
        activityRule.scenario.onActivity { activity ->
            val androidContent = activity.findViewById<ViewGroup>(android.R.id.content)
            val root = androidContent.getChildAt(0) as FrameLayout
            val capsule = (0 until root.childCount)
                .map(root::getChildAt)
                .filterIsInstance<LinearLayout>()
                .single { candidate ->
                    candidate.childCount == 5 &&
                        (0 until candidate.childCount).all { index ->
                            val control = candidate.getChildAt(index) as? TextView
                            control?.contentDescription?.toString()
                                ?.substringBefore(", selected") in PRIMARY_NAVIGATION_LABELS
                        }
                }
            val controls = (0 until capsule.childCount)
                .map(capsule::getChildAt)
                .filterIsInstance<TextView>()

            assertTrue("Trash navigation should keep five equal destination slots", controls.size == 5)
            assertTrue(
                "Trash should honor icons-only centered glyph presentation",
                controls.all { control ->
                    control.text.isNullOrEmpty() &&
                        control.compoundDrawables[0] != null &&
                        control.compoundDrawables[1] == null &&
                        (control.gravity and Gravity.CENTER) == Gravity.CENTER &&
                        !control.includeFontPadding
                },
            )
            val widths = controls.map { it.width }
            assertTrue(
                "Trash navigation slots should remain equal width: $widths",
                widths.maxOrNull()!! - widths.minOrNull()!! <= 1,
            )
            assertTrue(
                "Trash selected destination should use compact inset Glaze material",
                controls.single { it.isSelected }.background is InsetDrawable,
            )
            assertTrue(
                "Trash navigation should use the same bounded ripple feedback",
                controls.all { it.foreground is RippleDrawable },
            )
        }
    }

    @Test
    fun recycleBinContentStaysInsideSystemBarAndGestureSafeAreas() {
        activityRule.scenario.onActivity { activity ->
            val androidContent = activity.findViewById<ViewGroup>(android.R.id.content)
            val root = androidContent.getChildAt(0) as FrameLayout
            val insets = root.rootWindowInsets
            assertNotNull("Trash root must receive Android window insets", insets)

            val safe = currentSafeInsets(insets!!)
            val decorRect = Rect().also { activity.window.decorView.getGlobalVisibleRect(it) }
            val scroll = (0 until root.childCount)
                .map(root::getChildAt)
                .filterIsInstance<ScrollView>()
                .single()
            val scrollRect = Rect().also { scroll.getGlobalVisibleRect(it) }

            assertTrue(
                "Trash content must start below the status-bar/cutout safe edge",
                scrollRect.top >= decorRect.top + safe.top,
            )
            assertTrue(
                "Trash content must stay inside the physical left safe edge",
                scrollRect.left >= decorRect.left + safe.left,
            )
            assertTrue(
                "Trash content must stay inside the physical right safe edge",
                scrollRect.right <= decorRect.right - safe.right,
            )
            assertTrue(
                "Trash content must stay above the navigation/gesture safe edge",
                scrollRect.bottom <= decorRect.bottom - safe.bottom,
            )
        }
    }

    private fun currentSafeInsets(insets: WindowInsets): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val barsAndCutout = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val gestures = insets.getInsets(WindowInsets.Type.mandatorySystemGestures())
            return Rect(
                maxOf(barsAndCutout.left, gestures.left),
                maxOf(barsAndCutout.top, gestures.top),
                maxOf(barsAndCutout.right, gestures.right),
                maxOf(barsAndCutout.bottom, gestures.bottom),
            )
        }

        @Suppress("DEPRECATION")
        val cutout = insets.displayCutout
        @Suppress("DEPRECATION")
        return Rect(
            maxOf(insets.systemWindowInsetLeft, cutout?.safeInsetLeft ?: 0),
            maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0),
            maxOf(insets.systemWindowInsetRight, cutout?.safeInsetRight ?: 0),
            maxOf(insets.systemWindowInsetBottom, cutout?.safeInsetBottom ?: 0),
        )
    }

    private val PRIMARY_NAVIGATION_LABELS =
        setOf("Photos", "Albums", "Videos", "Trash", "Settings")

    private fun hasIconOnlyGlyph() = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("icon-only control with a centered compound drawable")
        }

        override fun matchesSafely(view: View): Boolean {
            val control = view as? TextView ?: return false
            return control.text.isNullOrEmpty() &&
                control.compoundDrawables[0] != null &&
                (control.gravity and Gravity.CENTER) == Gravity.CENTER
        }
    }

    private fun hasMinimumTouchSizeDp(minimumDp: Float) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("has rendered width and height of at least $minimumDp dp")
        }

        override fun matchesSafely(view: View): Boolean {
            val minimumPx = minimumDp * view.resources.displayMetrics.density
            return view.width >= minimumPx && view.height >= minimumPx
        }
    }

}
