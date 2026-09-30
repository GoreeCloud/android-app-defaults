package com.goreecloud.camera.settings

import android.content.Context

class CameraSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCompositionGridEnabled(): Boolean =
        preferences.getBoolean(KEY_COMPOSITION_GRID_ENABLED, false)

    fun setCompositionGridEnabled(enabled: Boolean) {
        check(
            preferences.edit()
                .putBoolean(KEY_COMPOSITION_GRID_ENABLED, enabled)
                .commit()
        ) { "Failed to persist Camera settings." }
    }

    fun selfTimerSeconds(): Int =
        CameraSelfTimerPolicy.normalizeSeconds(
            preferences.getInt(KEY_SELF_TIMER_SECONDS, CameraSelfTimerPolicy.OFF_SECONDS),
        )

    fun setSelfTimerSeconds(seconds: Int) {
        require(seconds in CameraSelfTimerPolicy.supportedSeconds) {
            "Unsupported Camera self-timer duration."
        }
        check(
            preferences.edit()
                .putInt(KEY_SELF_TIMER_SECONDS, seconds)
                .commit()
        ) { "Failed to persist Camera settings." }
    }

    fun isVolumeShutterEnabled(): Boolean =
        preferences.getBoolean(KEY_VOLUME_SHUTTER_ENABLED, true)

    fun setVolumeShutterEnabled(enabled: Boolean) {
        check(
            preferences.edit()
                .putBoolean(KEY_VOLUME_SHUTTER_ENABLED, enabled)
                .commit()
        ) { "Failed to persist Camera settings." }
    }

    companion object {
        const val PREFERENCES_NAME = "goreecloud_camera_settings"
        const val KEY_COMPOSITION_GRID_ENABLED = "composition_grid_enabled"
        const val KEY_SELF_TIMER_SECONDS = "self_timer_seconds"
        const val KEY_VOLUME_SHUTTER_ENABLED = "volume_shutter_enabled"
    }
}
