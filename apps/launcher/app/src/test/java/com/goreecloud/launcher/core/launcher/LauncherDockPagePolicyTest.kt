package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherDockPagePolicyTest {
    @Test
    fun preferredFiveUsesOnePageForStarterDock() {
        assertEquals(
            LauncherDockPagePlan(5, 5, 1),
            launcherDockPagePlan(
                itemCount = 5,
                configuredPageSize = 5,
                availableAppWidthDp = 320f,
                minimumInteractionTargetDp = 48f,
            ),
        )
    }

    @Test
    fun overflowCreatesIndependentDockPages() {
        assertEquals(
            LauncherDockPagePlan(5, 5, 3),
            launcherDockPagePlan(
                itemCount = 12,
                configuredPageSize = 5,
                availableAppWidthDp = 320f,
                minimumInteractionTargetDp = 48f,
            ),
        )
    }

    @Test
    fun narrowLayoutsPageBeforeViolatingInteractionFloor() {
        assertEquals(
            LauncherDockPagePlan(7, 4, 2),
            launcherDockPagePlan(
                itemCount = 7,
                configuredPageSize = 7,
                availableAppWidthDp = 200f,
                minimumInteractionTargetDp = 48f,
            ),
        )
    }

    @Test
    fun configuredDensityIsBounded() {
        assertEquals(4, launcherDockPagePlan(1, 1, 500f, 48f).configuredPageSize)
        assertEquals(7, launcherDockPagePlan(1, 99, 500f, 48f).configuredPageSize)
    }
}
