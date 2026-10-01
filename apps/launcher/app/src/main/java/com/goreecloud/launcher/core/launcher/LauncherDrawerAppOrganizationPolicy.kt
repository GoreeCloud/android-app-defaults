package com.goreecloud.launcher.core.launcher

/**
 * Local-only App Drawer organization policy.
 *
 * Keys are Launcher workspace keys, which already include Android profile identity. This keeps
 * hide/pin choices exact to one app component in one profile without changing LauncherApps
 * inventory authority, Home/Dock placement, folders, package state, or portable preference v1.
 */
object LauncherDrawerAppOrganizationPolicy {
    fun <T> visiblePinnedFirst(
        items: List<T>,
        hiddenKeys: Set<String>,
        pinnedKeys: Set<String>,
        keyOf: (T) -> String,
    ): List<T> {
        val visible = items.filterNot { keyOf(it) in hiddenKeys }
        if (visible.isEmpty() || pinnedKeys.isEmpty()) return visible
        val (pinned, regular) = visible.partition { keyOf(it) in pinnedKeys }
        return pinned + regular
    }

    fun <T> hiddenItems(
        items: List<T>,
        hiddenKeys: Set<String>,
        keyOf: (T) -> String,
    ): List<T> = items.filter { keyOf(it) in hiddenKeys }

    fun normalizedPinnedKeys(
        hiddenKeys: Set<String>,
        pinnedKeys: Set<String>,
    ): Set<String> = pinnedKeys - hiddenKeys
}
