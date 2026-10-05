package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerDiscoveryPolicyTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = 100L * day

    @Test
    fun recentInstallWindowIsBoundedAndFutureTimestampsFailClosed() {
        assertTrue(LauncherDrawerDiscoveryPolicy.recentlyInstalled(now - 2L * day, now))
        assertFalse(LauncherDrawerDiscoveryPolicy.recentlyInstalled(now - 31L * day, now))
        assertFalse(LauncherDrawerDiscoveryPolicy.recentlyInstalled(now + day, now))
        assertFalse(LauncherDrawerDiscoveryPolicy.recentlyInstalled(null, now))
    }

    @Test
    fun updateRequiresARealPostInstallChangeInsideWindow() {
        assertTrue(
            LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                LauncherAppFreshness(
                    firstInstallTimeMillis = now - 20L * day,
                    lastUpdateTimeMillis = now - day,
                ),
                now,
            ),
        )
        assertFalse(
            LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                LauncherAppFreshness(
                    firstInstallTimeMillis = now - day,
                    lastUpdateTimeMillis = now - day,
                ),
                now,
            ),
        )
        assertFalse(
            LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                LauncherAppFreshness(
                    firstInstallTimeMillis = now - 80L * day,
                    lastUpdateTimeMillis = now - 40L * day,
                ),
                now,
            ),
        )
    }

    @Test
    fun suggestedAppsUseTruthfulUsageThenDeterministicFallback() {
        val keys = linkedSetOf("camera", "browser", "mail", "maps")
        val labels = mapOf(
            "camera" to "Camera",
            "browser" to "Browser",
            "mail" to "Mail",
            "maps" to "Maps",
        )

        assertEquals(
            listOf("browser", "camera", "mail", "maps"),
            LauncherDrawerDiscoveryPolicy.suggestedKeys(
                availableKeys = keys,
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = labels,
            ).toList(),
        )

        assertEquals(
            listOf("maps", "camera", "mail", "browser"),
            LauncherDrawerDiscoveryPolicy.suggestedKeys(
                availableKeys = keys,
                recentAppKeys = listOf("maps", "camera"),
                launchCounts = mapOf("mail" to 9L, "browser" to 1L),
                labelByKey = labels,
            ).toList(),
        )
    }

    @Test
    fun filterKeysKeepsPinnedAndFreshnessRulesSeparate() {
        val keys = linkedSetOf("camera", "browser", "mail")
        val installTimes = mapOf(
            "camera" to now - 2L * day,
            "browser" to now - 50L * day,
            "mail" to now - 10L * day,
        )
        val freshness = mapOf(
            "browser" to LauncherAppFreshness(
                firstInstallTimeMillis = now - 100L * day,
                lastUpdateTimeMillis = now - day,
            ),
        )

        assertEquals(
            setOf("mail"),
            LauncherDrawerDiscoveryPolicy.filterKeys(
                filter = LauncherDrawerDiscoveryFilter.PINNED,
                availableKeys = keys,
                pinnedKeys = setOf("mail"),
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = emptyMap(),
                installTimeByKey = installTimes,
                freshnessByKey = freshness,
                nowMillis = now,
            ),
        )
        assertEquals(
            setOf("camera", "mail"),
            LauncherDrawerDiscoveryPolicy.filterKeys(
                filter = LauncherDrawerDiscoveryFilter.NEW,
                availableKeys = keys,
                pinnedKeys = emptySet(),
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = emptyMap(),
                installTimeByKey = installTimes,
                freshnessByKey = freshness,
                nowMillis = now,
            ),
        )
        assertEquals(
            setOf("browser"),
            LauncherDrawerDiscoveryPolicy.filterKeys(
                filter = LauncherDrawerDiscoveryFilter.UPDATED,
                availableKeys = keys,
                pinnedKeys = emptySet(),
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = emptyMap(),
                installTimeByKey = installTimes,
                freshnessByKey = freshness,
                nowMillis = now,
            ),
        )
    }
}
