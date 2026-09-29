package com.goreecloud.gallery

import android.content.Context

class GallerySetupPreferences(
    private val context: Context,
) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun shouldShowSetup(): Boolean {
        if (preferences.contains(ONBOARDING_COMPLETE_KEY)) {
            return !preferences.getBoolean(ONBOARDING_COMPLETE_KEY, false)
        }

        if (isUpgradeInstall()) {
            preferences.edit()
                .putBoolean(ONBOARDING_COMPLETE_KEY, true)
                .putInt(ONBOARDING_STEP_KEY, 0)
                .commit()
            return false
        }

        return true
    }

    fun onboardingStep(): Int =
        preferences.getInt(ONBOARDING_STEP_KEY, 0).coerceIn(0, STEP_COUNT - 1)

    fun setOnboardingStep(step: Int) {
        preferences.edit()
            .putInt(ONBOARDING_STEP_KEY, step.coerceIn(0, STEP_COUNT - 1))
            .commit()
    }

    fun completeSetup() {
        preferences.edit()
            .putBoolean(ONBOARDING_COMPLETE_KEY, true)
            .putInt(ONBOARDING_STEP_KEY, 0)
            .commit()
    }

    fun contextualHintsEnabled(): Boolean =
        preferences.getBoolean(CONTEXTUAL_HINTS_ENABLED_KEY, true)

    fun setContextualHintsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(CONTEXTUAL_HINTS_ENABLED_KEY, enabled)
            .commit()
    }

    fun isContextualHintDismissed(hintId: String): Boolean =
        preferences.getStringSet(DISMISSED_CONTEXTUAL_HINTS_KEY, emptySet())
            ?.contains(hintId) == true

    fun dismissContextualHint(hintId: String) {
        val dismissed = preferences
            .getStringSet(DISMISSED_CONTEXTUAL_HINTS_KEY, emptySet())
            .orEmpty()
            .toMutableSet()
        dismissed += hintId
        preferences.edit()
            .putStringSet(DISMISSED_CONTEXTUAL_HINTS_KEY, dismissed)
            .commit()
    }

    fun resetDismissedContextualHints() {
        preferences.edit()
            .remove(DISMISSED_CONTEXTUAL_HINTS_KEY)
            .commit()
    }

    private fun isUpgradeInstall(): Boolean =
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.lastUpdateTime > info.firstInstallTime
        }.getOrDefault(false)

    companion object {
        const val PREFERENCES_NAME = "goreecloud_gallery_local_state"
        const val ONBOARDING_COMPLETE_KEY = "startup_wizard_completed_v1"
        const val ONBOARDING_STEP_KEY = "startup_wizard_step_v1"
        const val CONTEXTUAL_HINTS_ENABLED_KEY = "contextual_hints_enabled_v1"
        const val DISMISSED_CONTEXTUAL_HINTS_KEY = "dismissed_contextual_hints_v1"
        const val HINT_PHOTOS = "photos"
        const val HINT_VIDEOS = "videos"
        const val HINT_ALBUMS = "albums"
        const val STEP_COUNT = 3
    }
}
