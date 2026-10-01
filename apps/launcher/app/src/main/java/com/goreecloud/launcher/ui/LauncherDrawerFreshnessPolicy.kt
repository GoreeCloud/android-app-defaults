package com.goreecloud.launcher.ui

internal enum class LauncherDrawerFreshnessFilter(val displayName: String) {
    ALL("All"),
    RECENTLY_INSTALLED("New"),
    RECENTLY_UPDATED("Updated"),
}

internal data class LauncherDrawerPackageFreshness(
    val firstInstallTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
)

internal object LauncherDrawerFreshnessPolicy {
    const val DEFAULT_WINDOW_MILLIS: Long = 30L * 24L * 60L * 60L * 1_000L
    private const val INITIAL_INSTALL_UPDATE_TOLERANCE_MILLIS: Long = 60_000L

    fun matches(
        filter: LauncherDrawerFreshnessFilter,
        freshness: LauncherDrawerPackageFreshness?,
        nowMillis: Long,
        windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    ): Boolean {
        if (filter == LauncherDrawerFreshnessFilter.ALL) return true
        if (freshness == null || windowMillis <= 0L) return false
        val cutoff = nowMillis - windowMillis
        return when (filter) {
            LauncherDrawerFreshnessFilter.ALL -> true
            LauncherDrawerFreshnessFilter.RECENTLY_INSTALLED ->
                freshness.firstInstallTimeMillis > 0L &&
                    freshness.firstInstallTimeMillis >= cutoff
            LauncherDrawerFreshnessFilter.RECENTLY_UPDATED ->
                freshness.lastUpdateTimeMillis >= cutoff &&
                    freshness.lastUpdateTimeMillis >
                        freshness.firstInstallTimeMillis +
                            INITIAL_INSTALL_UPDATE_TOLERANCE_MILLIS
        }
    }
}
