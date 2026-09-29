package com.goreecloud.keyboard

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Versioned Development portability boundary for explicitly selected, low-sensitivity
 * Keyboard preferences.
 *
 * The current schema intentionally contains only the last explicitly selected emoji
 * category. It must not be expanded to include typed text, composing context, suggestions,
 * search queries, clipboard contents, emoji recents, or other usage-derived history without
 * a separately reviewed privacy contract.
 */
object KeyboardPortablePreferences {
    const val FORMAT = "goreecloud-keyboard-preferences"
    const val VERSION = 1

    private const val MAX_SNAPSHOT_BYTES = 4096

    data class Snapshot(
        val emojiCategory: EmojiCategory,
    )

    sealed interface DecodeResult {
        data class Success(val snapshot: Snapshot) : DecodeResult
        data class Invalid(val reason: String) : DecodeResult
    }

    fun encode(snapshot: Snapshot): String {
        val payload = listOf(
            "format=$FORMAT",
            "version=$VERSION",
            "emoji_category=${snapshot.emojiCategory.name}",
        ).joinToString("\n")
        return "$payload\nchecksum=${sha256Hex(payload)}\n"
    }

    fun decode(encoded: String): DecodeResult {
        if (encoded.toByteArray(StandardCharsets.UTF_8).size > MAX_SNAPSHOT_BYTES) {
            return DecodeResult.Invalid("snapshot exceeds the bounded size limit")
        }
        if ('\r' in encoded) {
            return DecodeResult.Invalid("snapshot must use canonical LF line endings")
        }

        val canonical = encoded.removeSuffix("\n")
        val lines = canonical.split('\n')
        if (lines.size != 4 || lines.any { it.isBlank() }) {
            return DecodeResult.Invalid("snapshot must contain exactly the supported records")
        }

        if (lines[0] != "format=$FORMAT") {
            return DecodeResult.Invalid("unsupported snapshot format")
        }
        if (lines[1] != "version=$VERSION") {
            return DecodeResult.Invalid("unsupported snapshot version")
        }
        if (!lines[2].startsWith("emoji_category=")) {
            return DecodeResult.Invalid("emoji category record is missing")
        }
        if (!lines[3].startsWith("checksum=")) {
            return DecodeResult.Invalid("snapshot checksum record is missing")
        }

        val checksum = lines[3].removePrefix("checksum=")
        if (!checksum.matches(Regex("[0-9a-f]{64}"))) {
            return DecodeResult.Invalid("snapshot checksum is not canonical SHA-256")
        }
        val payload = lines.take(3).joinToString("\n")
        if (sha256Hex(payload) != checksum) {
            return DecodeResult.Invalid("snapshot integrity check failed")
        }

        val storedCategory = lines[2].removePrefix("emoji_category=")
        val category = EmojiCategory.entries.firstOrNull { it.name == storedCategory }
            ?: return DecodeResult.Invalid("emoji category is not supported")

        return DecodeResult.Success(Snapshot(category))
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
