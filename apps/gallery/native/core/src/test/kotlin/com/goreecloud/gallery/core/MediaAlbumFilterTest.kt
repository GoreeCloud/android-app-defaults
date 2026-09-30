package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaAlbumFilterTest {
    private fun item(id: String, albumId: String?, albumName: String?): MediaItem = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        capturedAt = null,
        modifiedAt = Instant.parse("2026-08-30T20:00:00Z"),
        width = 100,
        height = 100,
        durationMillis = null,
        sizeBytes = 1000,
        albumId = albumId,
        albumName = albumName,
    )

    @Test
    fun albumOptionsPreserveAuthorizedSnapshotOrderAndCounts() {
        val items = listOf(
            item("one", "camera", "Camera"),
            item("two", "screenshots", "Screenshots"),
            item("three", "camera", "Camera"),
            item("four", null, null),
        )

        assertEquals(
            listOf(
                MediaAlbumOption("camera", "Camera", 2),
                MediaAlbumOption("screenshots", "Screenshots", 1),
            ),
            mediaAlbumOptions(items),
        )
    }

    @Test
    fun albumFilterNeverExpandsBeyondAuthorizedSnapshot() {
        val items = listOf(
            item("one", "camera", "Camera"),
            item("two", "screenshots", "Screenshots"),
            item("three", null, null),
        )

        assertEquals(listOf("one"), filterAuthorizedAlbum(items, "camera").map { it.id })
        assertEquals(items, filterAuthorizedAlbum(items, null))
        assertEquals(emptyList<MediaItem>(), filterAuthorizedAlbum(items, "missing"))
    }
}
