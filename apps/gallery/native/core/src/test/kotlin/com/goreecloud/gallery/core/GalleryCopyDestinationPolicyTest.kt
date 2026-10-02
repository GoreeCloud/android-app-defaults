package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GalleryCopyDestinationPolicyTest {
    @Test
    fun `copy destinations come only from other authoritative album paths`() {
        val camera = media("1", "camera", "Camera", "DCIM/Camera/")
        val screenshots = media("2", "screens", "Screenshots", "Pictures/Screenshots/")
        val downloads = media("3", "downloads", "Download", "Download/")
        val noPath = media("4", "legacy", "Legacy", null)

        val destinations = GalleryCopyDestinationPolicy.existingDestinations(
            currentScope = listOf(camera, screenshots, downloads, noPath),
            selectedContentUris = setOf(camera.contentUri),
        )

        assertEquals(listOf("Download", "Screenshots"), destinations.map { it.displayName })
        assertEquals(listOf("Download/", "Pictures/Screenshots/"), destinations.map { it.relativePath })
    }

    @Test
    fun `all source paths are excluded for mixed-folder selection`() {
        val camera = media("1", "camera", "Camera", "DCIM/Camera/")
        val downloads = media("2", "downloads", "Download", "Download/")
        val trips = media("3", "trips", "Trips", "Pictures/Trips/")

        val destinations = GalleryCopyDestinationPolicy.existingDestinations(
            currentScope = listOf(camera, downloads, trips),
            selectedContentUris = setOf(camera.contentUri, downloads.contentUri),
        )

        assertEquals(listOf("Trips"), destinations.map { it.displayName })
    }

    @Test
    fun `destinations stay on the selected concrete MediaStore volume`() {
        val camera = media("1", "camera", "Camera", "DCIM/Camera/", volumeName = "external_primary")
        val removableTrips = media("2", "trips", "Trips", "Pictures/Trips/", volumeName = "1234-5678")

        assertTrue(
            GalleryCopyDestinationPolicy.existingDestinations(
                currentScope = listOf(camera, removableTrips),
                selectedContentUris = setOf(camera.contentUri),
            ).isEmpty(),
        )
    }

    @Test
    fun `cross volume selections fail closed`() {
        val camera = media("1", "camera", "Camera", "DCIM/Camera/", volumeName = "external_primary")
        val removable = media("2", "removable", "Removable", "Pictures/Removable/", volumeName = "1234-5678")
        val trips = media("3", "trips", "Trips", "Pictures/Trips/", volumeName = "external_primary")

        assertTrue(
            GalleryCopyDestinationPolicy.existingDestinations(
                currentScope = listOf(camera, removable, trips),
                selectedContentUris = setOf(camera.contentUri, removable.contentUri),
            ).isEmpty(),
        )
    }

    @Test
    fun `invalid source path fails closed instead of manufacturing Copy authority`() {
        val camera = media("1", "camera", "Camera", null)
        val trips = media("2", "trips", "Trips", "Pictures/Trips/")

        assertTrue(
            GalleryCopyDestinationPolicy.existingDestinations(
                currentScope = listOf(camera, trips),
                selectedContentUris = setOf(camera.contentUri),
            ).isEmpty(),
        )
    }

    @Test
    fun `foreign selection fails closed`() {
        val camera = media("1", "camera", "Camera", "DCIM/Camera/")
        val trips = media("2", "trips", "Trips", "Pictures/Trips/")

        assertTrue(
            GalleryCopyDestinationPolicy.existingDestinations(
                currentScope = listOf(camera, trips),
                selectedContentUris = setOf(camera.contentUri, "content://foreign/not-authorized"),
            ).isEmpty(),
        )
    }

    @Test
    fun `conflicting provider metadata cannot become copy authority`() {
        val selected = media("1", "camera", "Camera", "DCIM/Camera/")
        val conflictA = media("2", "shared", "Shared", "Pictures/One/")
        val conflictB = media("3", "shared", "Shared", "Pictures/Two/")

        assertTrue(
            GalleryCopyDestinationPolicy.existingDestinations(
                currentScope = listOf(selected, conflictA, conflictB),
                selectedContentUris = setOf(selected.contentUri),
            ).isEmpty(),
        )
    }

    private fun media(
        id: String,
        albumId: String,
        albumName: String,
        relativePath: String?,
        volumeName: String = "external_primary",
    ) = MediaItem(
        id = id,
        contentUri = "content://media/external/images/media/$id",
        displayName = "item-$id.jpg",
        mimeType = "image/jpeg",
        capturedAt = Instant.parse("2026-10-01T12:00:00Z"),
        modifiedAt = Instant.parse("2026-10-01T12:00:00Z"),
        width = 100,
        height = 100,
        durationMillis = null,
        sizeBytes = 100,
        albumId = albumId,
        albumName = albumName,
        volumeName = volumeName,
        relativePath = relativePath,
    )
}
