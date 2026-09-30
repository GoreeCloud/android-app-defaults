package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPortablePreferenceExportTest {
    @Test
    fun exportReadsExactlyOneExplicitCategoryAndRoundTrips() {
        var reads = 0
        val encoded = KeyboardPortablePreferenceExport.create(
            EmojiCategoryPreferenceReader {
                reads += 1
                EmojiCategory.TRAVEL
            },
        )

        assertEquals(1, reads)
        val decoded = KeyboardPortablePreferences.decode(encoded)
        assertTrue(decoded is KeyboardPortablePreferences.DecodeResult.Success)
        assertEquals(
            EmojiCategory.TRAVEL,
            (decoded as KeyboardPortablePreferences.DecodeResult.Success).snapshot.emojiCategory,
        )
    }

    @Test
    fun exportContainsNoUsageDerivedOrEditorDataRecords() {
        val encoded = KeyboardPortablePreferenceExport.create(
            EmojiCategoryPreferenceReader { EmojiCategory.SMILEYS },
        )

        for (forbidden in listOf(
            "emoji_recents",
            "typed_text",
            "composing",
            "surrounding_text",
            "clipboard",
            "suggestion",
            "search_query",
            "key_history",
            "telemetry",
            "identity",
        )) {
            assertFalse("portable export unexpectedly contains $forbidden", encoded.contains(forbidden))
        }
    }

    @Test
    fun exportUsesCanonicalPortableFormat() {
        val encoded = KeyboardPortablePreferenceExport.create(
            EmojiCategoryPreferenceReader { EmojiCategory.PEOPLE },
        )

        assertTrue(encoded.startsWith("format=${KeyboardPortablePreferences.FORMAT}\n"))
        assertTrue(encoded.contains("version=${KeyboardPortablePreferences.VERSION}\n"))
        assertTrue(encoded.contains("emoji_category=PEOPLE\n"))
        assertEquals(4, encoded.trimEnd().lines().size)
    }
}
