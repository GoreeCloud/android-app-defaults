package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun lettersExposeExpectedQwertyRows() {
        val rows = KeyboardLayout.characterRows(KeyboardLayer.LETTERS)

        assertEquals("qwertyuiop", rows[0].joinToString(""))
        assertEquals("asdfghjkl", rows[1].joinToString(""))
        assertEquals("zxcvbnm", rows[2].joinToString(""))
    }

    @Test
    fun primarySymbolsExposeDigitsAndCommonPunctuationWithoutLetters() {
        val rows = KeyboardLayout.characterRows(KeyboardLayer.SYMBOLS)
        val keys = rows.flatten()

        assertEquals("1234567890", rows[0].joinToString(""))
        assertTrue(keys.contains("@"))
        assertTrue(keys.contains("?"))
        assertTrue(keys.contains("!"))
        assertFalse(keys.any { key -> key.codePoints().anyMatch { Character.isLetter(it) } })
    }

    @Test
    fun secondarySymbolsExposeBracketsOperatorsCurrencyAndTypographyWithoutLetters() {
        val rows = KeyboardLayout.characterRows(KeyboardLayer.SYMBOLS_MORE)
        val keys = rows.flatten()

        for (key in listOf("[", "]", "{", "}", "<", ">", "=", "\\", "|", "~", "€", "£", "¥", "…", "—", "°")) {
            assertTrue("expected secondary symbol $key", keys.contains(key))
        }
        assertFalse(keys.any { key -> key.codePoints().anyMatch { Character.isLetter(it) } })
    }

    @Test
    fun emojiCategoriesExposeBoundedComposedSequencesThatDeleteAsWholeTextUnits() {
        val keysByCategory = EmojiCategory.entries.associateWith { category ->
            KeyboardLayout.emojiRows(category).flatten()
        }
        val allKeys = keysByCategory.values.flatten()

        for (key in listOf("😀", "😂", "❤️", "👍🏽", "🙏🏾", "👩‍💻", "👨‍👩‍👧‍👦", "🏳️‍🌈", "🇺🇸", "🚀", "🐶", "🌿", "🍎", "🍕", "✈️", "🚗", "🧳", "🏕️")) {
            assertTrue("expected emoji $key", allKeys.contains(key))
        }
        assertTrue(allKeys.any { key -> key.length == 2 && key.codePointCount(0, key.length) == 1 })
        assertTrue(allKeys.any { key -> key.codePointCount(0, key.length) > 1 })
        assertTrue(allKeys.any { key -> key.codePoints().anyMatch { it == 0x200D } })
        assertTrue(allKeys.any { key -> key.codePoints().anyMatch { it in 0x1F3FB..0x1F3FF } })
        assertTrue(allKeys.any { key -> key.codePointCount(0, key.length) == 2 && key.codePoints().allMatch { it in 0x1F1E6..0x1F1FF } })

        for ((category, keys) in keysByCategory) {
            assertEquals("$category must expose three emoji rows", 3, KeyboardLayout.emojiRows(category).size)
            assertEquals("$category must remain bounded", 24, keys.size)
            for (key in keys) {
                assertEquals(
                    "backspace must consume the complete emoji key $key from $category",
                    key.codePointCount(0, key.length),
                    TextDeletion.previousTextUnitCodePointCount(key),
                )
            }
        }
    }

    @Test
    fun emojiCategoriesCycleDeterministicallyWithoutPersistence() {
        assertEquals(EmojiCategory.PEOPLE, KeyboardLayout.nextEmojiCategory(EmojiCategory.SMILEYS))
        assertEquals(EmojiCategory.NATURE, KeyboardLayout.nextEmojiCategory(EmojiCategory.PEOPLE))
        assertEquals(EmojiCategory.FOOD, KeyboardLayout.nextEmojiCategory(EmojiCategory.NATURE))
        assertEquals(EmojiCategory.TRAVEL, KeyboardLayout.nextEmojiCategory(EmojiCategory.FOOD))
        assertEquals(EmojiCategory.SYMBOLS, KeyboardLayout.nextEmojiCategory(EmojiCategory.TRAVEL))
        assertEquals(EmojiCategory.SMILEYS, KeyboardLayout.nextEmojiCategory(EmojiCategory.SYMBOLS))
    }

    @Test
    fun travelCategoryIsOfflineAndBounded() {
        val travel = KeyboardLayout.emojiRows(EmojiCategory.TRAVEL).flatten()

        assertEquals(24, travel.size)
        assertTrue(travel.containsAll(listOf("🚗", "✈️", "🚆", "🚢", "🏖️", "🗺️", "🧳")))
    }

    @Test
    fun everyLayerReturnsThreeTextRows() {
        for (layer in KeyboardLayer.entries) {
            val rows: List<List<String>> = KeyboardLayout.characterRows(layer)
            assertEquals("$layer must expose three text rows", 3, rows.size)
            assertTrue(rows.flatten().all { it.isNotEmpty() })
        }
    }
}