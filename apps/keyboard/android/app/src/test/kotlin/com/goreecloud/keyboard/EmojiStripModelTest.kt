package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiStripModelTest {
    @Test
    fun categoryOnlyStripIncludesLocalSearchAndCompleteAccessibleNames() {
        val entries = EmojiStripModel.entries(hasRecents = false)

        assertEquals(EmojiCategory.entries.size + 1, entries.size)
        assertTrue(entries.first().search)
        assertEquals("Search emoji", entries.first().accessibilityLabel)
        assertEquals(EmojiCategory.entries.toList(), entries.drop(1).mapNotNull { it.category })
        assertTrue(entries.all { it.visibleLabel.length <= 2 })
        assertTrue(entries.drop(1).all { it.accessibilityLabel.endsWith(" emoji") })
        assertFalse(entries.any { it.recent || it.clearRecents })
    }

    @Test
    fun recentControlsFollowSearchAndPrecedeCategoriesOnlyWhenRecentsExist() {
        val entries = EmojiStripModel.entries(hasRecents = true)

        assertEquals(EmojiCategory.entries.size + 3, entries.size)
        assertTrue(entries[0].search)
        assertEquals("Search emoji", entries[0].accessibilityLabel)
        assertTrue(entries[1].recent)
        assertEquals("Recent emoji", entries[1].accessibilityLabel)
        assertTrue(entries[2].clearRecents)
        assertEquals("Clear recent emoji", entries[2].accessibilityLabel)
        assertEquals(EmojiCategory.entries.toList(), entries.drop(3).mapNotNull { it.category })
    }
}
