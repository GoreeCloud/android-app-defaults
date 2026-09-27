package com.goreecloud.clock.data

import android.content.Context
import android.text.format.DateFormat
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

data class ClockPreferences(
    val theme: ThemePreference,
    val use24Hour: Boolean,
    val clockFace: ClockFacePreference,
    val hapticsEnabled: Boolean,
    val reducedMotion: Boolean,
    val worldZones: List<String>,
)

class ClockPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("clock_preferences", Context.MODE_PRIVATE)
    private val default24Hour = DateFormat.is24HourFormat(context)

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

    private fun update(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        mutableState.value = read()
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
        )
    }

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
    }
}
