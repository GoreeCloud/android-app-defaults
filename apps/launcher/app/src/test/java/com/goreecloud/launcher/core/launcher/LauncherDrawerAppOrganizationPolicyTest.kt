package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerAppOrganizationPolicyTest {
    private data class Item(val key: String)

    @Test
    fun hiddenAppsAreExcludedAndPinnedAppsStayAheadOfRegularOrder() {
        val items = listOf(Item("0:a"), Item("0:b"), Item("0:c"), Item("0:d"))

        val result = LauncherDrawerAppOrganizationPolicy.visiblePinnedFirst(
            items = items,
            hiddenKeys = setOf("0:c"),
            pinnedKeys = setOf("0:b", "0:d"),
            keyOf = Item::key,
        )

        assertEquals(listOf("0:b", "0:d", "0:a"), result.map(Item::key))
    }

    @Test
    fun hiddenStateWinsIfAStaleKeyIsAlsoPinned() {
        val items = listOf(Item("0:a"), Item("0:b"))

        val result = LauncherDrawerAppOrganizationPolicy.visiblePinnedFirst(
            items = items,
            hiddenKeys = setOf("0:b"),
            pinnedKeys = setOf("0:b"),
            keyOf = Item::key,
        )

        assertEquals(listOf("0:a"), result.map(Item::key))
        assertEquals(
            emptySet<String>(),
            LauncherDrawerAppOrganizationPolicy.normalizedPinnedKeys(
                hiddenKeys = setOf("0:b"),
                pinnedKeys = setOf("0:b"),
            ),
        )
    }

    @Test
    fun hiddenManagerReturnsOnlyHiddenInstalledItems() {
        val items = listOf(Item("0:a"), Item("0:b"), Item("10:a"))

        val hidden = LauncherDrawerAppOrganizationPolicy.hiddenItems(
            items = items,
            hiddenKeys = setOf("0:b", "10:a", "0:missing"),
            keyOf = Item::key,
        )

        assertEquals(listOf("0:b", "10:a"), hidden.map(Item::key))
    }

    @Test
    fun profileQualifiedKeysDoNotCrossAffectSameComponentInAnotherProfile() {
        val personal = "0:com.example/.Main"
        val work = "10:com.example/.Main"
        val result = LauncherDrawerAppOrganizationPolicy.visiblePinnedFirst(
            items = listOf(Item(personal), Item(work)),
            hiddenKeys = setOf(work),
            pinnedKeys = setOf(personal),
            keyOf = Item::key,
        )

        assertEquals(listOf(personal), result.map(Item::key))
        assertTrue(personal in result.map(Item::key))
        assertFalse(work in result.map(Item::key))
    }
}
