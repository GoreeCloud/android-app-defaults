package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthorizedMediaSearchTest {
    private val now = Instant.parse("2026-09-29T12:00:00Z")

    private fun media(
        id: String,
        name: String,
        mimeType: String = "image/jpeg",
        albumId: String? = null,
        albumName: String? = null,
    ) = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = name,
        mimeType = mimeType,
        capturedAt = now,
        modifiedAt = now,
        width = 100,
        height = 100,
        durationMillis = if (mimeType.startsWith("video/")) 1_000 else null,
        sizeBytes = 10,
        albumId = albumId,
        albumName = albumName,
    )

    @Test
    fun searchesOnlyMetadataFromAuthorizedSnapshot() {
        val items = listOf(
            media("1", "Beach sunset.jpg", albumId = "a", albumName = "Vacation"),
            media("2", "Receipt.jpg", albumId = "b", albumName = "Documents"),
        )
        assertEquals(listOf("1"), AuthorizedMediaSearch.search(items, "vacation beach").map { it.id })
    }

    @Test
    fun matchesKindAndMimeWithoutChangingSnapshotOrder() {
        val items = listOf(
            media("1", "clip one.mp4", mimeType = "video/mp4"),
            media("2", "clip two.mp4", mimeType = "video/mp4"),
            media("3", "photo.jpg"),
        )
        assertEquals(listOf("1", "2"), AuthorizedMediaSearch.search(items, "video").map { it.id })
        assertEquals(listOf("1", "2"), AuthorizedMediaSearch.search(items, "mp4").map { it.id })
    }

    @Test
    fun blankQueryPreservesBrowsingWhileActiveSearchRemainsBounded() {
        val items = (1..120).map { media(it.toString(), "photo-$it.jpg") }
        assertEquals(items.map { it.id }, AuthorizedMediaSearch.search(items, "  ", limit = 3).map { it.id })
        assertEquals(
            AuthorizedMediaSearch.MAX_RESULTS,
            AuthorizedMediaSearch.search(items, "photo", limit = 500).size,
        )
    }
}
