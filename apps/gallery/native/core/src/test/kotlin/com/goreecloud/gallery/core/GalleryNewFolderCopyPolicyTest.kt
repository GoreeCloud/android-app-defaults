package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GalleryNewFolderCopyPolicyTest {
    @Test
    fun `copy can create one photo folder from mixed source folders`() {
        val download = media("1", "downloads", "Download", "Download/")
        val screenshots = media("2", "screens", "Screenshots", "Pictures/Screenshots/")

        val destination = GalleryNewFolderCopyPolicy.destinationForSelection(
            currentScope = listOf(download, screenshots),
            selectedContentUris = setOf(download.contentUri, screenshots.contentUri),
            rawFolderName = "Trip Photos",
        )

        assertEquals("Pictures", destination.parentDisplayName)
        assertEquals("Pictures/Trip Photos/", destination.relativePath)
    }

    @Test
    fun `mixed image and video copy uses DCIM`() {
        val image = media("1", "downloads", "Download", "Download/")
        val video = media("2", "movies", "Movies", "Movies/", mimeType = "video/mp4")

        val destination = GalleryNewFolderCopyPolicy.destinationForSelection(
            currentScope = listOf(image, video),
            selectedContentUris = setOf(image.contentUri, video.contentUri),
            rawFolderName = "Trip",
        )

        assertEquals("DCIM", destination.parentDisplayName)
        assertEquals("DCIM/Trip/", destination.relativePath)
    }

    @Test
    fun `video-only copy uses Movies`() {
        val video = media("1", "movies", "Movies", "Movies/", mimeType = "video/mp4")
        assertEquals(
            "Movies",
            GalleryNewFolderCopyPolicy.parentForSelection(
                currentScope = listOf(video),
                selectedContentUris = setOf(video.contentUri),
            ),
        )
    }

    @Test
    fun `foreign selection cannot establish new folder copy authority`() {
        val image = media("1", "downloads", "Download", "Download/")
        assertNull(
            GalleryNewFolderCopyPolicy.parentForSelection(
                currentScope = listOf(image),
                selectedContentUris = setOf(image.contentUri, "content://foreign/not-authorized"),
            ),
        )
    }

    @Test
    fun `visible destination collision fails closed`() {
        val image = media("1", "downloads", "Download", "Download/")
        val existing = media("2", "trip", "Trip", "Pictures/Trip/")
        assertFailsWith<IllegalArgumentException> {
            GalleryNewFolderCopyPolicy.destinationForSelection(
                currentScope = listOf(image, existing),
                selectedContentUris = setOf(image.contentUri),
                rawFolderName = "trip",
            )
        }
    }

    private fun media(
        id: String,
        albumId: String,
        albumName: String,
        relativePath: String?,
        mimeType: String = "image/jpeg",
    ) = MediaItem(
        id = id,
        contentUri = if (mimeType.startsWith("video/")) {
            "content://media/external/video/media/$id"
        } else {
            "content://media/external/images/media/$id"
        },
        displayName = if (mimeType.startsWith("video/")) "item-$id.mp4" else "item-$id.jpg",
        mimeType = mimeType,
        capturedAt = Instant.parse("2026-10-01T12:00:00Z"),
        modifiedAt = Instant.parse("2026-10-01T12:00:00Z"),
        width = 100,
        height = 100,
        durationMillis = if (mimeType.startsWith("video/")) 1000 else null,
        sizeBytes = 100,
        albumId = albumId,
        albumName = albumName,
        relativePath = relativePath,
    )
}
