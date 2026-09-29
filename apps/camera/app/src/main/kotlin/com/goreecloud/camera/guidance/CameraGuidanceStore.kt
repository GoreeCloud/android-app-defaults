// File internal version: 0.1.0
package com.goreecloud.camera.guidance

import android.content.Context

class CameraGuidanceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isFirstUseComplete(): Boolean =
        preferences.getBoolean(KEY_FIRST_USE_COMPLETE, false)

    fun currentStep(): Int =
        CameraGuidancePolicy.normalizeStep(preferences.getInt(KEY_CURRENT_STEP, 0))

    fun setCurrentStep(step: Int) {
        preferences.edit()
            .putInt(KEY_CURRENT_STEP, CameraGuidancePolicy.normalizeStep(step))
            .apply()
    }

    fun completeFirstUse() {
        preferences.edit()
            .putBoolean(KEY_FIRST_USE_COMPLETE, true)
            .putInt(KEY_CURRENT_STEP, 0)
            .apply()
    }

    fun restartGuide() {
        preferences.edit()
            .putInt(KEY_CURRENT_STEP, 0)
            .apply()
    }

    fun areContextualHintsEnabled(): Boolean =
        preferences.getBoolean(KEY_CONTEXTUAL_HINTS_ENABLED, true)

    fun setContextualHintsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_CONTEXTUAL_HINTS_ENABLED, enabled)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "goreecloud_camera_guidance"
        const val KEY_FIRST_USE_COMPLETE = "first_use_complete"
        const val KEY_CURRENT_STEP = "current_step"
        const val KEY_CONTEXTUAL_HINTS_ENABLED = "contextual_hints_enabled"
    }
}
