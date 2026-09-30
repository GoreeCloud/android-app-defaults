package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineEmojiSearchTest {
    @Test
    fun findsRepresentativeLocalEmojiByPlainLanguageKeywords() {
        assertTrue(OfflineEmojiSearch.search("laugh").any { it.emoji == "😂" })
        assertTrue(OfflineEmojiSearch.search("dog").any { it.emoji == "🐶" })
        assertTrue(OfflineEmojiSearch.search("pizza").any { it.emoji == "🍕" })
        assertTrue(OfflineEmojiSearch.search("airplane travel").any { it.emoji == "✈️" })
        assertTrue(OfflineEmojiSearch.search("heart love").any { it.emoji == "❤️" })
    }

    @Test
    fun exactEmojiQueryFindsTheEmojiWithoutNetworkMetadata() {
        assertEquals("🧳", OfflineEmojiSearch.search("🧳").single().emoji)
    }

    @Test
    fun categoryNameCanDiscoverBoundedResults() {
        val travel = OfflineEmojiSearch.search("travel", limit = 8)
        assertEquals(8, travel.size)
        assertTrue(travel.all { it.category == EmojiCategory.TRAVEL || it.emoji == "🚀" })
    }

    @Test
    fun emptyAndZeroLimitQueriesReturnNoResults() {
        assertTrue(OfflineEmojiSearch.search("   ").isEmpty())
        assertTrue(OfflineEmojiSearch.search("travel", limit = 0).isEmpty())
    }

    @Test
    fun resultLimitNeverExceedsPrivacyBoundedMaximum() {
        assertTrue(OfflineEmojiSearch.search("travel", limit = 999).size <= OfflineEmojiSearch.MAX_RESULTS)
    }
}
