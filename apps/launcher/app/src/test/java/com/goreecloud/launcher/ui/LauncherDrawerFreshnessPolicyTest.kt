package com.goreecloud.launcher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerFreshnessPolicyTest {
    private val now = 2_000_000_000_000L
    private val day = 24L * 60L * 60L * 1_000L

    @Test
    fun allAlwaysIncludesAppsEvenWithoutFreshnessMetadata() {
        assertTrue(
            LauncherDrawerFreshnessPolicy.matches(
                filter = LauncherDrawerFreshnessFilter.ALL,
                freshness = null,
                nowMillis = now,
            ),
        )
    }

    @Test
    fun recentlyInstalledUsesThirtyDayWindow() {
        val recent = LauncherDrawerPackageFreshness(
            firstInstallTimeMillis = now - 5L * day,
            lastUpdateTimeMillis = now - 5L * day,
        )
        val old = LauncherDrawerPackageFreshness(
            firstInstallTimeMillis = now - 45L * day,
            lastUpdateTimeMillis = now - 2L * day,
        )

        assertTrue(
            LauncherDrawerFreshnessPolicy.matches(
                LauncherDrawerFreshnessFilter.RECENTLY_INSTALLED,
                recent,
                now,
            ),
        )
        assertFalse(
            LauncherDrawerFreshnessPolicy.matches(
                LauncherDrawerFreshnessFilter.RECENTLY_INSTALLED,
                old,
                now,
            ),
        )
    }

    @Test
    fun recentlyUpdatedExcludesInitialInstallAndIncludesMeaningfulLaterUpdate() {
        val initialInstall = LauncherDrawerPackageFreshness(
            firstInstallTimeMillis = now - 2L * day,
            lastUpdateTimeMillis = now - 2L * day + 10_000L,
        )
        val updated = LauncherDrawerPackageFreshness(
            firstInstallTimeMillis = now - 90L * day,
            lastUpdateTimeMillis = now - day,
        )

        assertFalse(
            LauncherDrawerFreshnessPolicy.matches(
                LauncherDrawerFreshnessFilter.RECENTLY_UPDATED,
                initialInstall,
                now,
            ),
        )
        assertTrue(
            LauncherDrawerFreshnessPolicy.matches(
                LauncherDrawerFreshnessFilter.RECENTLY_UPDATED,
                updated,
                now,
            ),
        )
    }

    @Test
    fun missingMetadataFailsClosedForFreshnessFilters() {
        assertFalse(
            LauncherDrawerFreshnessPolicy.matches(
                LauncherDrawerFreshnessFilter.RECENTLY_INSTALLED,
                freshness = null,
                nowMillis = now,
            ),
        )
        assertFalse(
            LauncherDrawerFreshnessPolicy.matches(
                LauncherDrawerFreshnessFilter.RECENTLY_UPDATED,
                freshness = null,
                nowMillis = now,
            ),
        )
    }
}
