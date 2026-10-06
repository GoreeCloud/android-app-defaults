package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GalleryAlbumRenamePolicyTest {
    @Test
    fun `nested album rename preserves provider parent and exact bounded items`() {
        val first = media("1", "camera", "Camera", "external_primary", "DCIM/Camera/")
        val second = media("2", "camera", "Camera", "external_primary", "DCIM/Camera/")

        val destination = GalleryAlbumRenamePolicy.destinationForAlbum(
            currentScope = listOf(first, second),
            albumId = "camera",
            rawName = "  Family Camera  ",
        )

        assertEquals("Camera", destination.source.currentName)
        assertEquals("DCIM/", destination.source.parentRelativePath)
        assertEquals(listOf(first.contentUri, second.contentUri), destination.source.contentUris)
        assertEquals("Family Camera", destination.newName)
        assertEquals("DCIM/Family Camera/", destination.destinationRelativePath)
    }

    @Test
    fun `root-level provider folder is not exposed as rename authority`() {
        val download = media("1", "download", "Download", "external_primary", "Download/")

        assertNull(
            GalleryAlbumRenamePolicy.sourceForAlbum(
                currentScope = listOf(download),
                albumId = "download",
            ),
        )
    }

    @Test
    fun `mixed paths or volumes fail closed`() {
        val first = media("1", "trip", "Trip", "external_primary", "Pictures/Trip/")
        val differentPath = media("2", "trip", "Trip", "external_primary", "DCIM/Trip/")
        val differentVolume = media("3", "trip", "Trip", "0123-4567", "Pictures/Trip/")

        assertNull(GalleryAlbumRenamePolicy.sourceForAlbum(listOf(first, differentPath), "trip"))
        assertNull(GalleryAlbumRenamePolicy.sourceForAlbum(listOf(first, differentVolume), "trip"))
    }

    @Test
    fun `provider album name must agree with path leaf`() {
        val item = media("1", "trip", "Vacation", "external_primary", "Pictures/Trip/")

        assertNull(GalleryAlbumRenamePolicy.sourceForAlbum(listOf(item), "trip"))
    }

    @Test
    fun `same name and sibling collision are rejected`() {
        val trip = media("1", "trip", "Trip", "external_primary", "Pictures/Trip/")
        val family = media("2", "family", "Family", "external_primary", "Pictures/Family/")

        assertFailsWith<IllegalArgumentException> {
            GalleryAlbumRenamePolicy.destinationForAlbum(listOf(trip, family), "trip", "trip")
        }
        assertFailsWith<IllegalArgumentException> {
            GalleryAlbumRenamePolicy.destinationForAlbum(listOf(trip, family), "trip", "family")
        }
    }

    @Test
    fun `same folder name on another volume does not create a false collision`() {
        val trip = media("1", "trip", "Trip", "external_primary", "Pictures/Trip/")
        val familyOnCard = media("2", "family", "Family", "0123-4567", "Pictures/Family/")

        val destination = GalleryAlbumRenamePolicy.destinationForAlbum(
            currentScope = listOf(trip, familyOnCard),
            albumId = "trip",
            rawName = "Family",
        )

        assertEquals("Pictures/Family/", destination.destinationRelativePath)
    }

    private fun media(
        id: String,
        albumId: String,
        albumName: String,
        volumeName: String?,
        relativePath: String?,
    ): MediaItem = MediaItem(
        id = id,
        contentUri = "content://media/external/images/media/$id",
        displayName = "item-$id.jpg",
        mimeType = "image/jpeg",
        capturedAt = Instant.parse("2026-10-05T12:00:00Z"),
        modifiedAt = Instant.parse("2026-10-05T12:00:00Z"),
        width = 1080,
        height = 1920,
        durationMillis = null,
        sizeBytes = 1024,
        albumId = albumId,
        albumName = albumName,
        volumeName = volumeName,
        relativePath = relativePath,
    )
}
