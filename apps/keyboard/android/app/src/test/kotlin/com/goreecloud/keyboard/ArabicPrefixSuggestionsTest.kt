package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicPrefixSuggestionsTest {
    @Test
    fun ranksExactArabicPrefixCompletionsInStableLocalOrder() {
        assertEquals(
            listOf("من", "مع", "ماذا"),
            ArabicPrefixSuggestions.suggest("م"),
        )
    }

    @Test
    fun exactKnownWordStaysFirstBeforeLongerCompletions() {
        val result = ArabicPrefixSuggestions.suggest("هذا")

        assertEquals("هذا", result.first())
    }

    @Test
    fun offersOnlyWordsThatShareTheTypedArabicPrefix() {
        val result = ArabicPrefixSuggestions.suggest("رسا")

        assertEquals(listOf("رسالة", "رسائل"), result)
        assertTrue(result.all { it.startsWith("رسا") })
    }

    @Test
    fun normalizesCanonicallyEquivalentArabicPrefix() {
        val decomposedHamzaPrefix = "أر"

        assertEquals(
            listOf("أريد", "أرى"),
            ArabicPrefixSuggestions.suggest(decomposedHamzaPrefix),
        )
    }

    @Test
    fun nonArabicMixedAndDiacriticPrefixesFailClosed() {
        assertEquals(emptyList<String>(), ArabicPrefixSuggestions.suggest("a"))
        assertEquals(emptyList<String>(), ArabicPrefixSuggestions.suggest("مa"))
        assertEquals(emptyList<String>(), ArabicPrefixSuggestions.suggest("مَ"))
    }

    @Test
    fun blankUnknownAndNonPositiveLimitReturnNoCandidates() {
        assertEquals(emptyList<String>(), ArabicPrefixSuggestions.suggest(""))
        assertEquals(emptyList<String>(), ArabicPrefixSuggestions.suggest("زززز"))
        assertEquals(emptyList<String>(), ArabicPrefixSuggestions.suggest("م", limit = 0))
    }
}
