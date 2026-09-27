package com.goreecloud.clock.data

import android.content.Context
import android.text.format.DateFormat
import com.goreecloud.clock.widget.ClockWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ClockFacePreference {
    DIGITAL,
    ANALOG,
}

enum class OnboardingStep(val id: String) {
    WELCOME("welcome"),
    TIME_DISPLAY("time_display"),
    GUIDANCE("guidance"),
    READY("ready");

    companion object {
        fun fromId(raw: String?): OnboardingStep =
            entries.firstOrNull { it.id == raw } ?: WELCOME
    }
}

object ClockHintIds {
    const val ALARMS_RELIABILITY = "alarms_reliability_v1"
    const val TIMER_RELIABILITY = "timer_reliability_v1"
}

data class ClockPreferences(
    val theme: ThemePreference,
    val use24Hour: Boolean,
    val clockFace: ClockFacePreference,
    val hapticsEnabled: Boolean,
    val reducedMotion: Boolean,
    val worldZones: List<String>,
    val onboardingCompleted: Boolean,
    val onboardingStep: OnboardingStep,
    val hintsEnabled: Boolean,
    val dismissedHints: Set<String>,
)

class ClockPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("clock_preferences", Context.MODE_PRIVATE)
    private val default24Hour = DateFormat.is24HourFormat(appContext)

    private val mutableState = MutableStateFlow(read())
    val state = mutableState.asStateFlow()

    fun setTheme(value: ThemePreference) = update {
        putString(KEY_THEME, value.name)
    }

    fun setUse24Hour(value: Boolean) = update {
        putBoolean(KEY_24_HOUR, value)
    }

    fun setClockFace(value: ClockFacePreference) = update {
        putString(KEY_CLOCK_FACE, value.name)
    }

    fun setHapticsEnabled(value: Boolean) = update {
        putBoolean(KEY_HAPTICS, value)
    }

    fun setReducedMotion(value: Boolean) = update {
        putBoolean(KEY_REDUCED_MOTION, value)
    }

    fun setWorldZones(zones: List<String>) = update {
        putString(KEY_WORLD_ZONES, zones.distinct().joinToString(","))
    }

    fun setOnboardingStep(step: OnboardingStep) = update(refreshWidgets = false) {
        putString(KEY_ONBOARDING_STEP, step.id)
    }

    fun completeOnboarding() = update(refreshWidgets = false) {
        putBoolean(KEY_ONBOARDING_COMPLETED, true)
        putString(KEY_ONBOARDING_STEP, OnboardingStep.READY.id)
    }

    fun replayOnboarding() = update(refreshWidgets = false) {
        putBoolean(KEY_ONBOARDING_COMPLETED, false)
        putString(KEY_ONBOARDING_STEP, OnboardingStep.WELCOME.id)
    }

    fun setHintsEnabled(value: Boolean) = update(refreshWidgets = false) {
        putBoolean(KEY_HINTS_ENABLED, value)
    }

    fun dismissHint(id: String) = update(refreshWidgets = false) {
        putStringSet(KEY_DISMISSED_HINTS, readDismissedHints() + id)
    }

    fun resetDismissedHints() = update(refreshWidgets = false) {
        putStringSet(KEY_DISMISSED_HINTS, emptySet())
    }

    private fun update(
        refreshWidgets: Boolean = true,
        block: android.content.SharedPreferences.Editor.() -> Unit,
    ) {
        prefs.edit().apply(block).apply()
        mutableState.value = read()
        if (refreshWidgets) {
            ClockWidgetUpdater.updateAll(appContext)
        }
    }

    private fun read(): ClockPreferences {
        val theme = enumValueOrDefault(
            prefs.getString(KEY_THEME, null),
            ThemePreference.SYSTEM,
        )
        val clockFace = enumValueOrDefault(
            prefs.getString(KEY_CLOCK_FACE, null),
            ClockFacePreference.DIGITAL,
        )
        val zones = prefs.getString(KEY_WORLD_ZONES, "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }

        return ClockPreferences(
            theme = theme,
            use24Hour = prefs.getBoolean(KEY_24_HOUR, default24Hour),
            clockFace = clockFace,
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            reducedMotion = prefs.getBoolean(KEY_REDUCED_MOTION, false),
            worldZones = zones,
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
            onboardingStep = OnboardingStep.fromId(prefs.getString(KEY_ONBOARDING_STEP, null)),
            hintsEnabled = prefs.getBoolean(KEY_HINTS_ENABLED, true),
            dismissedHints = readDismissedHints(),
        )
    }

    private fun readDismissedHints(): Set<String> =
        prefs.getStringSet(KEY_DISMISSED_HINTS, emptySet()).orEmpty().toSet()

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String?,
        default: T,
    ): T = runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(default)

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_24_HOUR = "use_24_hour"
        const val KEY_CLOCK_FACE = "clock_face"
        const val KEY_HAPTICS = "haptics"
        const val KEY_REDUCED_MOTION = "reduced_motion"
        const val KEY_WORLD_ZONES = "world_zones"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed_v1"
        const val KEY_ONBOARDING_STEP = "onboarding_step_v1"
        const val KEY_HINTS_ENABLED = "contextual_hints_enabled_v1"
        const val KEY_DISMISSED_HINTS = "dismissed_hint_ids_v1"
    }
}
