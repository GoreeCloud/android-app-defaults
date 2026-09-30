package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiCategoryPreferenceTest {
    @Test
    fun roundTripPreservesEverySupportedCategory() {
        EmojiCategory.entries.forEach { category ->
            assertEquals(category, EmojiCategoryPreference.decode(EmojiCategoryPreference.encode(category)))
        }
    }

    @Test
    fun missingOrUnknownValuesFailClosedToSmileys() {
        assertEquals(EmojiCategory.SMILEYS, EmojiCategoryPreference.decode(null))
        assertEquals(EmojiCategory.SMILEYS, EmojiCategoryPreference.decode(""))
        assertEquals(EmojiCategory.SMILEYS, EmojiCategoryPreference.decode("NOT_A_CATEGORY"))
    }
}
