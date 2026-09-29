package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizedMediaViewSummaryTest {
    private fun item(id: String, mimeType: String, albumId: String, albumName: String): MediaItem = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = "$id.file",
        mimeType = mimeType,
        capturedAt = null,
        modifiedAt = Instant.parse("2026-08-31T12:00:00Z"),
        width = 100,
        height = 100,
        durationMillis = if (mimeType.startsWith("video/")) 1_000 else null,
        sizeBytes = 1_024,
        albumId = albumId,
        albumName = albumName,
    )

    @Test
    fun summaryCountsOnlyThePresentedAuthorizedView() {
        val items = listOf(
            item("camera-image", "image/jpeg", "camera", "Camera"),
            item("camera-video", "video/mp4", "camera", "Camera"),
            item("shot-image", "image/png", "screenshots", "Screenshots"),
        )

        val summary = summarizeAuthorizedMediaView(
            items,
            MediaTypeFilter.IMAGES,
            MediaSortOrder.OLDEST,
            "camera",
        )

        assertEquals(3, summary.authorizedCount)
        assertEquals(1, summary.presentedCount)
        assertEquals("Camera", summary.albumName)
        assertEquals(MediaTypeFilter.IMAGES, summary.mediaTypeFilter)
        assertEquals(MediaSortOrder.OLDEST, summary.sortOrder)
        assertTrue(summary.hasNonDefaultControls)
    }

    @Test
    fun defaultViewNeedsNoReset() {
        val items = listOf(item("one", "image/jpeg", "camera", "Camera"))
        val summary = summarizeAuthorizedMediaView(items, MediaTypeFilter.ALL, MediaSortOrder.NEWEST, null)

        assertEquals(1, summary.presentedCount)
        assertFalse(summary.hasNonDefaultControls)
    }
}