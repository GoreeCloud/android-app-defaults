package com.goreecloud.clock.ui

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class ClockHapticEvent(val feedbackConstant: Int) {
    ACTION(HapticFeedbackConstants.VIRTUAL_KEY),
    TICK(HapticFeedbackConstants.CLOCK_TICK),
}

@Composable
fun rememberClockHapticFeedback(
    enabled: Boolean,
): (ClockHapticEvent) -> Unit {
    val view = LocalView.current
    return remember(view, enabled) {
        { event ->
            if (enabled) {
                view.performHapticFeedback(event.feedbackConstant)
            }
        }
    }
}
