package com.goreecloud.gallery.core

import java.time.Instant

data class MediaAlbum(
    val id: String,
    val displayName: String,
    val itemCount: Int,
    val coverItemId: String,
    val newestAt: Instant,
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(itemCount > 0)
        require(coverItemId.isNotBlank())
    }
}

/**
 * Builds deterministic local album summaries from Android-owned media metadata.
 *
 * Items without an authoritative album id/name remain valid media but are not
 * fabricated into a synthetic album by the core domain. A future Android adapter
 * may supply MediaStore bucket metadata or another platform-authorized source.
 */
fun List<MediaItem>.buildAlbumCatalog(): List<MediaAlbum> {
    val grouped = asSequence()
        .filter { it.albumId != null && it.albumName != null }
        .groupBy { checkNotNull(it.albumId) }

    return grouped.map { (albumId, items) ->
        val names = items.map { checkNotNull(it.albumName) }.distinct()
        require(names.size == 1) { "album id $albumId has conflicting display names" }

        val cover = items.maxWithOrNull(
            compareBy<MediaItem> { it.capturedAt ?: it.modifiedAt }
                .thenBy { it.modifiedAt }
                .thenBy { it.id }
        ) ?: error("album groups must contain at least one media item")
        val newestAt = cover.capturedAt ?: cover.modifiedAt

        MediaAlbum(
            id = albumId,
            displayName = names.single(),
            itemCount = items.size,
            coverItemId = cover.id,
            newestAt = newestAt,
        )
    }.sortedWith(
        compareByDescending<MediaAlbum> { it.newestAt }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            .thenBy { it.id }
    )
}
