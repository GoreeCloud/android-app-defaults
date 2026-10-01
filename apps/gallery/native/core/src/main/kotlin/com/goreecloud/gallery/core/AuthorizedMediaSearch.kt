package com.goreecloud.gallery.core

import java.util.Locale

object AuthorizedMediaSearch {
    const val MAX_RESULTS = 100

    fun search(
        items: List<MediaItem>,
        query: String,
        limit: Int = MAX_RESULTS,
    ): List<MediaItem> {
        val boundedLimit = limit.coerceIn(0, MAX_RESULTS)
        if (boundedLimit == 0) return emptyList()

        val tokens = query
            .trim()
            .lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return items.take(boundedLimit)

        return items.asSequence()
            .filter { item ->
                val haystack = buildString {
                    append(item.displayName.lowercase(Locale.ROOT))
                    append(' ')
                    append(item.albumName?.lowercase(Locale.ROOT).orEmpty())
                    append(' ')
                    append(item.mimeType.lowercase(Locale.ROOT))
                    append(' ')
                    append(item.kind.name.lowercase(Locale.ROOT))
                }
                tokens.all(haystack::contains)
            }
            .take(boundedLimit)
            .toList()
    }
}
