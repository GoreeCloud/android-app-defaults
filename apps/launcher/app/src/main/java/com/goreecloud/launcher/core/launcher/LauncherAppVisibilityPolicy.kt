package com.goreecloud.launcher.core.launcher

/**
 * Device-local discovery policy for user-hidden applications.
 *
 * The key is the exact profile-qualified workspace identity. Hiding affects only discovery
 * surfaces; Home, Dock, folders, widgets, package state, and Room workspace authority continue
 * using the complete Android LauncherApps inventory.
 */
internal object LauncherAppVisibilityPolicy {
    fun isDiscoverable(
        appKey: String,
        hiddenAppKeys: Set<String>,
    ): Boolean = appKey !in hiddenAppKeys
}
