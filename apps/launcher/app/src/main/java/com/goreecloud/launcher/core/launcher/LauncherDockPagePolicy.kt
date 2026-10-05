package com.goreecloud.launcher.core.launcher

internal const val LAUNCHER_DOCK_DEFAULT_PAGE_SIZE = 5
internal const val LAUNCHER_DOCK_MIN_PAGE_SIZE = 4
internal const val LAUNCHER_DOCK_MAX_PAGE_SIZE = 7

internal data class LauncherDockPagePlan(
    val configuredPageSize: Int,
    val effectivePageSize: Int,
    val pageCount: Int,
)

/**
 * Computes Dock paging without ever shrinking an interactive slot below the resolved accessibility
 * floor. A user's preferred page size is therefore a density target, not permission to compress
 * touch targets. Narrow layouts page earlier instead.
 */
internal fun launcherDockPagePlan(
    itemCount: Int,
    configuredPageSize: Int,
    availableAppWidthDp: Float,
    minimumInteractionTargetDp: Float,
): LauncherDockPagePlan {
    val configured = configuredPageSize.coerceIn(
        LAUNCHER_DOCK_MIN_PAGE_SIZE,
        LAUNCHER_DOCK_MAX_PAGE_SIZE,
    )
    val safeByWidth = if (availableAppWidthDp <= 0f || minimumInteractionTargetDp <= 0f) {
        1
    } else {
        (availableAppWidthDp / minimumInteractionTargetDp).toInt().coerceAtLeast(1)
    }
    val effective = configured.coerceAtMost(safeByWidth).coerceAtLeast(1)
    val count = itemCount.coerceAtLeast(0)
    val pages = if (count == 0) 1 else (count + effective - 1) / effective
    return LauncherDockPagePlan(
        configuredPageSize = configured,
        effectivePageSize = effective,
        pageCount = pages,
    )
}
