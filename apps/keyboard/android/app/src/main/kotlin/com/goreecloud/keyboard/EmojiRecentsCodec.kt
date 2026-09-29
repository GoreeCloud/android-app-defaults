package com.goreecloud.keyboard

/**
 * Minimal on-device serialization for the bounded emoji-recent list.
 *
 * The current GoreeCloud emoji catalog never contains newline characters, so a
 * newline-delimited representation preserves exact multi-code-point emoji while
 * avoiding any network, analytics, or external serialization dependency.
 */
object EmojiRecentsCodec {
    fun encode(values: List<String>, limit: Int = EmojiRecents.DEFAULT_LIMIT): String =
        values.asSequence()
            .filter { it.isNotBlank() && !it.contains('\n') && !it.contains('\r') }
            .distinct()
            .take(limit.coerceAtLeast(1))
            .joinToString("\n")

    fun decode(serialized: String?, limit: Int = EmojiRecents.DEFAULT_LIMIT): List<String> {
        if (serialized.isNullOrEmpty()) return emptyList()
        return serialized.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(limit.coerceAtLeast(1))
            .toList()
    }
}
