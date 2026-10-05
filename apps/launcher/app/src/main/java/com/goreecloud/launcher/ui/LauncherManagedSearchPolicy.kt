package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherHomeSearchSurface

internal enum class LauncherManagedSearchReconciliationAction {
    ENSURE_PRESENT,
    REMOVE_MANAGED,
    NONE,
}

/**
 * Keeps the dedicated movable Search widget synchronized without issuing pointless mutations.
 *
 * In particular, Swipe down / fixed Search must not ask Room to remove a widget when no Search
 * widget is present. During HOME startup that old no-op request could race workspace authority and
 * surface a misleading "could not be removed" toast even though there was nothing to remove.
 */
internal fun launcherManagedSearchReconciliationAction(
    surface: LauncherHomeSearchSurface,
    hasAnyMovableSearch: Boolean,
): LauncherManagedSearchReconciliationAction = when {
    surface == LauncherHomeSearchSurface.MOVABLE && !hasAnyMovableSearch ->
        LauncherManagedSearchReconciliationAction.ENSURE_PRESENT
    surface != LauncherHomeSearchSurface.MOVABLE && hasAnyMovableSearch ->
        LauncherManagedSearchReconciliationAction.REMOVE_MANAGED
    else -> LauncherManagedSearchReconciliationAction.NONE
}
