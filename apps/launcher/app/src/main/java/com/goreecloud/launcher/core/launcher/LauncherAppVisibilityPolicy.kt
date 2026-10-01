package com.goreecloud.launcher.core.launcher

/**
 * Device-local discovery policy for user-hidden applications.
 *
 * The key must be the Launcher's exact profile-qualified workspace identity. Hiding affects only
 * discovery surfaces; callers that render persisted Home, Dock, or folder placement must continue
 * using the complete Android LauncherApps inventory.
 */
internal object LauncherAppVisibilityPolicy {
    fun isDiscoverable(
        appKey: String,
        hiddenAppKeys: Set<String>,
    ): Boolean = appKey !in hiddenAppKeys
}
