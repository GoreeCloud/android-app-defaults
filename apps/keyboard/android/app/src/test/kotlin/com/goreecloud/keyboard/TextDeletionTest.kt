package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class TextDeletionTest {
    @Test
    fun emptyInputDeletesNothing() {
        assertEquals(0, TextDeletion.previousTextUnitCodePointCount(""))
    }

    @Test
    fun plainSupplementaryEmojiDeletesOneCodePoint() {
        assertEquals(1, TextDeletion.previousTextUnitCodePointCount("hello😀"))
    }

    @Test
    fun emojiModifierStaysWithItsBase() {
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("ok👍🏽"))
    }

    @Test
    fun zwjEmojiSequenceDeletesAsOneBoundedTextUnit() {
        assertEquals(3, TextDeletion.previousTextUnitCodePointCount("work👩‍💻"))
        assertEquals(4, TextDeletion.previousTextUnitCodePointCount("work👩🏽‍💻"))
    }

    @Test
    fun variationSelectorAndKeycapStayWithTheirBase() {
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("love❤️"))
        assertEquals(3, TextDeletion.previousTextUnitCodePointCount("press1️⃣"))
    }

    @Test
    fun combiningMarkStaysWithItsBase() {
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("café"))
    }

    @Test
    fun regionalIndicatorPairDeletesAsOneFlagUnit() {
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("flag🇺🇸"))
    }

    @Test
    fun regionalIndicatorRunsPairFromTheStart() {
        assertEquals(1, TextDeletion.previousTextUnitCodePointCount("flags🇦🇧🇨"))
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("flags🇦🇧🇨🇩"))
        assertEquals(1, TextDeletion.previousTextUnitCodePointCount("flags🇦🇧🇨🇩🇪"))
    }

    @Test
    fun nonRegionalTextResetsRegionalIndicatorPairing() {
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("🇦🇧x🇨🇩"))
        assertEquals(1, TextDeletion.previousTextUnitCodePointCount("🇦🇧x🇨"))
    }

    @Test
    fun crlfDeletesAsOneLineBreakUnit() {
        assertEquals(2, TextDeletion.previousTextUnitCodePointCount("line\r\n"))
    }

    @Test
    fun truncatedContextRefusesTextUnitThatTouchesLookbehindBoundary() {
        val context = "a" + "\u0301".repeat(63)
        assertEquals(
            0,
            TextDeletion.previousTextUnitCodePointCount(
                textBeforeCursor = context,
                contextMayBeTruncated = true,
            ),
        )
    }

    @Test
    fun truncatedRegionalIndicatorRunRefusesUnknownPairingParity() {
        val context = buildString {
            repeat(16) { append("🇺🇸") }
        }
        assertEquals(
            0,
            TextDeletion.previousTextUnitCodePointCount(
                textBeforeCursor = context,
                contextMayBeTruncated = true,
            ),
        )
    }

    @Test
    fun truncatedWindowStillDeletesWhenUnitStartIsVisible() {
        val context = "x".repeat(63) + "y"
        assertEquals(
            1,
            TextDeletion.previousTextUnitCodePointCount(
                textBeforeCursor = context,
                contextMayBeTruncated = true,
            ),
        )
    }
}
