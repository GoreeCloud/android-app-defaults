package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaTypeFilterTest {
    private val items = listOf(
        mediaItem("image-1", "image/jpeg"),
        mediaItem("video-1", "video/mp4"),
        mediaItem("image-2", "image/png"),
    )

    @Test
    fun allPreservesAuthorizedSnapshotOrder() {
        assertEquals(items, MediaTypeFilter.ALL.filter(items))
    }

    @Test
    fun imagesKeepsOnlyImagesWithoutReordering() {
        assertEquals(listOf("image-1", "image-2"), MediaTypeFilter.IMAGES.filter(items).map { it.id })
    }

    @Test
    fun videosKeepsOnlyVideosWithoutReordering() {
        assertEquals(listOf("video-1"), MediaTypeFilter.VIDEOS.filter(items).map { it.id })
    }

    private fun mediaItem(id: String, mimeType: String) = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = "$id.file",
        mimeType = mimeType,
        capturedAt = null,
        modifiedAt = Instant.parse("2026-08-30T12:00:00Z"),
        width = 100,
        height = 100,
        durationMillis = if (mimeType.startsWith("video/")) 1_000 else null,
        sizeBytes = 1_024,
    )
}
