package com.goreecloud.keyboard

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPortablePreferencesTest {
    @Test
    fun roundTripCarriesOnlyExplicitEmojiCategoryPreference() {
        val category = EmojiCategory.entries.last()
        val encoded = KeyboardPortablePreferences.encode(
            KeyboardPortablePreferences.Snapshot(emojiCategory = category)
        )

        assertTrue(encoded.contains("emoji_category=${category.name}"))
        assertFalse(encoded.contains("emoji_recents"))
        assertFalse(encoded.contains("typed_text"))
        assertFalse(encoded.contains("suggestion"))
        assertFalse(encoded.contains("clipboard"))
        assertFalse(encoded.contains("search_query"))

        val decoded = KeyboardPortablePreferences.decode(encoded)
        assertTrue(decoded is KeyboardPortablePreferences.DecodeResult.Success)
        assertEquals(
            category,
            (decoded as KeyboardPortablePreferences.DecodeResult.Success).snapshot.emojiCategory,
        )
    }

    @Test
    fun checksumTamperingIsRejected() {
        val encoded = KeyboardPortablePreferences.encode(
            KeyboardPortablePreferences.Snapshot(EmojiCategory.SMILEYS)
        )
        val tampered = encoded.replace("emoji_category=SMILEYS", "emoji_category=UNKNOWN")

        val decoded = KeyboardPortablePreferences.decode(tampered)
        assertTrue(decoded is KeyboardPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "snapshot integrity check failed",
            (decoded as KeyboardPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun usageDerivedEmojiRecentsRecordIsRejectedEvenWithValidChecksum() {
        val payload = listOf(
            "format=${KeyboardPortablePreferences.FORMAT}",
            "version=${KeyboardPortablePreferences.VERSION}",
            "emoji_category=SMILEYS",
            "emoji_recents=😀,🔒",
        ).joinToString("\n")
        val encoded = "$payload\nchecksum=${sha256Hex(payload)}\n"

        val decoded = KeyboardPortablePreferences.decode(encoded)
        assertTrue(decoded is KeyboardPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "snapshot must contain exactly the supported records",
            (decoded as KeyboardPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun unknownCategoryIsRejectedWhenIntegrityIsValid() {
        val payload = listOf(
            "format=${KeyboardPortablePreferences.FORMAT}",
            "version=${KeyboardPortablePreferences.VERSION}",
            "emoji_category=NOT_A_CATEGORY",
        ).joinToString("\n")
        val encoded = "$payload\nchecksum=${sha256Hex(payload)}\n"

        val decoded = KeyboardPortablePreferences.decode(encoded)
        assertTrue(decoded is KeyboardPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "emoji category is not supported",
            (decoded as KeyboardPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun oversizedInputIsRejectedBeforeParsing() {
        val decoded = KeyboardPortablePreferences.decode("x".repeat(4097))
        assertTrue(decoded is KeyboardPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "snapshot exceeds the bounded size limit",
            (decoded as KeyboardPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
