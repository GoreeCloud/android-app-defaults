package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaCatalogTest {
    private fun item(name: String, mime: String, captured: Long) = MediaItem(
        id = name,
        contentUri = "content://media/$name",
        displayName = name,
        mimeType = mime,
        capturedAt = Instant.ofEpochSecond(captured),
        modifiedAt = Instant.ofEpochSecond(captured),
        width = 100,
        height = 100,
        durationMillis = if (mime.startsWith("video/")) 1000 else null,
        sizeBytes = 10,
    )

    @Test
    fun filtersAndSortsMedia() {
        val items = listOf(item("older.jpg", "image/jpeg", 1), item("newer.jpg", "image/jpeg", 2), item("clip.mp4", "video/mp4", 3))
        val result = items.applyCatalogRequest(CatalogRequest(kinds = setOf(MediaKind.IMAGE)))
        assertEquals(listOf("newer.jpg", "older.jpg"), result.map { it.displayName })
    }
}
