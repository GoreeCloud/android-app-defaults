package com.goreecloud.gallery.core

interface MediaCatalog {
    fun list(request: CatalogRequest): List<MediaItem>
}

data class CatalogRequest(
    val kinds: Set<MediaKind> = MediaKind.entries.toSet(),
    val query: String? = null,
    val sort: MediaSort = MediaSort.CAPTURED_DESC,
)

enum class MediaSort { CAPTURED_DESC, CAPTURED_ASC, MODIFIED_DESC, NAME_ASC }

fun List<MediaItem>.applyCatalogRequest(request: CatalogRequest): List<MediaItem> {
    val normalizedQuery = request.query?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
    val filtered = asSequence()
        .filter { it.kind in request.kinds }
        .filter { normalizedQuery == null || it.displayName.lowercase().contains(normalizedQuery) }

    val comparator = when (request.sort) {
        MediaSort.CAPTURED_DESC -> compareByDescending<MediaItem> { it.capturedAt ?: it.modifiedAt }
        MediaSort.CAPTURED_ASC -> compareBy<MediaItem> { it.capturedAt ?: it.modifiedAt }
        MediaSort.MODIFIED_DESC -> compareByDescending<MediaItem> { it.modifiedAt }
        MediaSort.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
    }

    return filtered.sortedWith(comparator).toList()
}
