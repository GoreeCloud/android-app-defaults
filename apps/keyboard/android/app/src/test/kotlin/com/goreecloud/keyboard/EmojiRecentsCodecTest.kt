package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EmojiRecentsCodecTest {
    @Test
    fun roundTripPreservesExactComposedEmojiAndOrder() {
        val values = listOf("👨‍👩‍👧‍👦", "🏳️‍🌈", "👍🏽", "🇸🇳")
        assertEquals(values, EmojiRecentsCodec.decode(EmojiRecentsCodec.encode(values)))
    }

    @Test
    fun encodingDeduplicatesAndEnforcesBound() {
        val values = listOf("😀", "😀", "😁", "😂")
        assertEquals(listOf("😀", "😁"), EmojiRecentsCodec.decode(EmojiRecentsCodec.encode(values, limit = 2), limit = 2))
    }

    @Test
    fun malformedLineBreakValuesAreNotPersisted() {
        val encoded = EmojiRecentsCodec.encode(listOf("😀", "bad\nvalue", "😁"))
        assertFalse(encoded.contains("bad"))
        assertEquals(listOf("😀", "😁"), EmojiRecentsCodec.decode(encoded))
    }

    @Test
    fun modelRestoresBoundedLocalValues() {
        val recents = EmojiRecents(limit = 2, initialValues = listOf("😀", "😁", "😂"))
        assertEquals(listOf("😀", "😁"), recents.values())
    }
}
