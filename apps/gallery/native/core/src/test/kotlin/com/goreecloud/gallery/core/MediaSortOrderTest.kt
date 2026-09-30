package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaSortOrderTest {
    private val older = mediaItem("older", "2026-08-28T12:00:00Z")
    private val newer = mediaItem("newer", "2026-08-30T12:00:00Z")
    private val tied = mediaItem("tied", "2026-08-30T12:00:00Z")

    @Test
    fun newestSortsDescendingAndPreservesInputOrderForTies() {
        val result = MediaSortOrder.NEWEST.sort(listOf(older, newer, tied))
        assertEquals(listOf("newer", "tied", "older"), result.map { it.id })
    }

    @Test
    fun oldestSortsAscendingAndPreservesInputOrderForTies() {
        val result = MediaSortOrder.OLDEST.sort(listOf(newer, tied, older))
        assertEquals(listOf("older", "newer", "tied"), result.map { it.id })
    }

    @Test
    fun capturedAtFallsBackToModifiedAt() {
        val fallback = MediaItem(
            id = "fallback",
            contentUri = "content://media/fallback",
            displayName = "fallback.jpg",
            mimeType = "image/jpeg",
            capturedAt = null,
            modifiedAt = Instant.parse("2026-08-29T12:00:00Z"),
            width = 100,
            height = 100,
            durationMillis = null,
            sizeBytes = 1024,
        )
        assertEquals(
            listOf("newer", "fallback", "older"),
            MediaSortOrder.NEWEST.sort(listOf(older, fallback, newer)).map { it.id },
        )
    }

    private fun mediaItem(id: String, timestamp: String) = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        capturedAt = Instant.parse(timestamp),
        modifiedAt = Instant.parse(timestamp),
        width = 100,
        height = 100,
        durationMillis = null,
        sizeBytes = 1024,
    )
}
