package com.goreecloud.keyboard

/**
 * Central fail-closed eligibility policy for privacy-bounded Spacebar cursor control.
 *
 * This policy intentionally does not inspect editor text. The caller supplies only the already-owned
 * editor sensitivity classification and presentation state.
 */
internal object SpacebarCursorAvailabilityPolicy {
    fun isEnabled(
        settingEnabled: Boolean,
        sensitiveInput: Boolean,
        layer: KeyboardLayer,
        touchExplorationEnabled: Boolean,
    ): Boolean =
        settingEnabled &&
            !sensitiveInput &&
            layer != KeyboardLayer.EMOJI &&
            !touchExplorationEnabled
}
