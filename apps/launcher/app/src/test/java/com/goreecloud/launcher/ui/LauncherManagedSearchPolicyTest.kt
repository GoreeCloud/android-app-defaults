package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherHomeSearchSurface
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherManagedSearchPolicyTest {
    @Test
    fun movableEnsuresSearchOnlyWhenMissing() {
        assertEquals(
            LauncherManagedSearchReconciliationAction.ENSURE_PRESENT,
            launcherManagedSearchReconciliationAction(
                LauncherHomeSearchSurface.MOVABLE,
                hasAnyMovableSearch = false,
            ),
        )
        assertEquals(
            LauncherManagedSearchReconciliationAction.NONE,
            launcherManagedSearchReconciliationAction(
                LauncherHomeSearchSurface.MOVABLE,
                hasAnyMovableSearch = true,
            ),
        )
    }

    @Test
    fun nonMovableDoesNotIssueRemovalWhenNoSearchExists() {
        assertEquals(
            LauncherManagedSearchReconciliationAction.NONE,
            launcherManagedSearchReconciliationAction(
                LauncherHomeSearchSurface.SWIPE_DOWN_ONLY,
                hasAnyMovableSearch = false,
            ),
        )
        assertEquals(
            LauncherManagedSearchReconciliationAction.NONE,
            launcherManagedSearchReconciliationAction(
                LauncherHomeSearchSurface.FIXED_BOTTOM,
                hasAnyMovableSearch = false,
            ),
        )
    }

    @Test
    fun nonMovableRemovesManagedInstanceWhenSearchExists() {
        assertEquals(
            LauncherManagedSearchReconciliationAction.REMOVE_MANAGED,
            launcherManagedSearchReconciliationAction(
                LauncherHomeSearchSurface.FIXED_TOP,
                hasAnyMovableSearch = true,
            ),
        )
    }
}
