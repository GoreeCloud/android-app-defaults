package com.goreecloud.keyboard

data class EmojiSearchSnapshot(
    val active: Boolean,
    val query: String,
    val results: List<EmojiSearchResult>,
)

class EmojiSearchSession(
    private val maxQueryCodePoints: Int = MAX_QUERY_CODE_POINTS,
) {
    private var active = false
    private var query = ""

    fun open(): EmojiSearchSnapshot {
        active = true
        return snapshot()
    }

    fun close(): EmojiSearchSnapshot {
        active = false
        query = ""
        return snapshot()
    }

    fun clear(): EmojiSearchSnapshot {
        query = ""
        return snapshot()
    }

    fun replaceQuery(value: String): EmojiSearchSnapshot {
        if (!active) return snapshot()
        query = value.trimStart().takeCodePoints(maxQueryCodePoints)
        return snapshot()
    }

    fun append(text: String): EmojiSearchSnapshot {
        if (!active || text.isEmpty()) return snapshot()
        val remaining = (maxQueryCodePoints - query.codePointCount(0, query.length)).coerceAtLeast(0)
        if (remaining == 0) return snapshot()
        query += text.takeCodePoints(remaining)
        return snapshot()
    }

    fun backspace(): EmojiSearchSnapshot {
        if (!active || query.isEmpty()) return snapshot()
        val lastCodePointStart = query.offsetByCodePoints(query.length, -1)
        query = query.substring(0, lastCodePointStart)
        return snapshot()
    }

    fun snapshot(): EmojiSearchSnapshot = EmojiSearchSnapshot(
        active = active,
        query = query,
        results = if (active) OfflineEmojiSearch.search(query) else emptyList(),
    )

    private fun String.takeCodePoints(limit: Int): String {
        if (limit <= 0 || isEmpty()) return ""
        val count = codePointCount(0, length)
        if (count <= limit) return this
        return substring(0, offsetByCodePoints(0, limit))
    }

    companion object {
        const val MAX_QUERY_CODE_POINTS = 48
    }
}
