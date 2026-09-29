package com.goreecloud.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionCapturePolicyTest {
    @Test
    fun acceptsPrefixUpToConfiguredCodePointBoundary() {
        assertTrue(SuggestionCapturePolicy.canAppend("he", "llo", maxCodePoints = 5))
        assertTrue(SuggestionCapturePolicy.canAppend("hello", "", maxCodePoints = 5))
    }

    @Test
    fun rejectsAppendThatWouldExceedBoundary() {
        assertFalse(SuggestionCapturePolicy.canAppend("hello", "s", maxCodePoints = 5))
        assertFalse(SuggestionCapturePolicy.canAppend("", "abcdef", maxCodePoints = 5))
    }

    @Test
    fun countsUnicodeCodePointsRatherThanUtf16Units() {
        val supplementaryLetter = String(Character.toChars(0x10400))
        assertTrue(SuggestionCapturePolicy.canAppend(supplementaryLetter, "a", maxCodePoints = 2))
        assertFalse(SuggestionCapturePolicy.canAppend(supplementaryLetter, "ab", maxCodePoints = 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveBoundary() {
        SuggestionCapturePolicy.canAppend("", "a", maxCodePoints = 0)
    }
}
