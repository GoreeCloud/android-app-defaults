package com.goreecloud.keyboard

/**
 * Returns how many Unicode code points should be deleted for one backspace action.
 *
 * This is a deliberately bounded text-unit model for the keyboard input surface. It keeps common
 * emoji sequences, combining marks, variation selectors, emoji modifiers, keycaps, tag sequences,
 * regional-indicator flags, and ZWJ-linked emoji together. Regional-indicator runs follow the
 * Unicode pair-from-the-start rule, so an odd trailing indicator is not incorrectly merged with
 * the preceding completed flag pair. It does not claim full UAX #29 grapheme segmentation for
 * every script.
 *
 * When [contextMayBeTruncated] is true, zero means the available lookbehind cannot prove the start
 * of the previous text unit. Callers must fail closed rather than deleting a partial sequence.
 */
object TextDeletion {
    fun previousTextUnitCodePointCount(
        textBeforeCursor: CharSequence,
        contextMayBeTruncated: Boolean = false,
    ): Int {
        if (textBeforeCursor.isEmpty()) return 0

        val codePoints = textBeforeCursor.toString().codePoints().toArray()
        if (codePoints.isEmpty()) return 0

        var start = baseStart(codePoints, codePoints.lastIndex)
        var trailingRegionalRunTouchesBoundary = false

        if (isRegionalIndicator(codePoints[start])) {
            val runStart = trailingRegionalIndicatorRunStart(codePoints, start)
            trailingRegionalRunTouchesBoundary = runStart == 0
            val runLength = start - runStart + 1
            if (runLength % 2 == 0) {
                start -= 1
            }
        }

        while (start > 1 && codePoints[start - 1] == ZERO_WIDTH_JOINER) {
            start = baseStart(codePoints, start - 2)
        }

        if (codePoints[start] == LINE_FEED && start > 0 && codePoints[start - 1] == CARRIAGE_RETURN) {
            start -= 1
        }

        if (contextMayBeTruncated && (start == 0 || trailingRegionalRunTouchesBoundary)) {
            return 0
        }

        return codePoints.size - start
    }

    private fun trailingRegionalIndicatorRunStart(codePoints: IntArray, finalIndicatorIndex: Int): Int {
        var start = finalIndicatorIndex
        while (start > 0 && isRegionalIndicator(codePoints[start - 1])) {
            start -= 1
        }
        return start
    }

    private fun baseStart(codePoints: IntArray, index: Int): Int {
        var start = index
        while (start > 0 && isExtendingCodePoint(codePoints[start])) {
            start -= 1
        }
        return start
    }

    private fun isExtendingCodePoint(codePoint: Int): Boolean {
        val type = Character.getType(codePoint)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            codePoint == VARIATION_SELECTOR_15 ||
            codePoint == VARIATION_SELECTOR_16 ||
            codePoint in VARIATION_SELECTOR_SUPPLEMENT ||
            codePoint in EMOJI_MODIFIERS ||
            codePoint == COMBINING_ENCLOSING_KEYCAP ||
            codePoint in TAG_CHARACTERS
    }

    private fun isRegionalIndicator(codePoint: Int): Boolean = codePoint in REGIONAL_INDICATORS

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val VARIATION_SELECTOR_15 = 0xFE0E
    private const val VARIATION_SELECTOR_16 = 0xFE0F
    private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3
    private const val CARRIAGE_RETURN = 0x000D
    private const val LINE_FEED = 0x000A

    private val VARIATION_SELECTOR_SUPPLEMENT = 0xE0100..0xE01EF
    private val EMOJI_MODIFIERS = 0x1F3FB..0x1F3FF
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
    private val TAG_CHARACTERS = 0xE0020..0xE007F
}
