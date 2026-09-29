package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryBrowseNavigationTest {
    @Test
    fun picturesAndAlbumsCountsComeOnlyFromAuthorizedMedia() {
        val items = listOf(
            media("1", "Camera", "camera", 3),
            media("2", "Camera", "camera", 2),
            media("3", "Screenshots", "screenshots", 1),
            media("4", null, null, 0),
        )

        val navigation = buildGalleryBrowseNavigation(items, GalleryBrowseSection.PICTURES)
        val pictures = navigation.destinations.single { it.section == GalleryBrowseSection.PICTURES }
        val albums = navigation.destinations.single { it.section == GalleryBrowseSection.ALBUMS }

        assertEquals(4, pictures.itemCount)
        assertEquals(2, albums.itemCount)
        assertTrue(pictures.selected)
        assertFalse(albums.selected)
    }

    @Test
    fun albumsSelectionDoesNotChangeUnderlyingMediaAuthority() {
        val items = listOf(media("1", "Camera", "camera", 1))
        val navigation = buildGalleryBrowseNavigation(items, GalleryBrowseSection.ALBUMS)

        assertEquals(GalleryBrowseSection.ALBUMS, navigation.selectedSection)
        assertTrue(navigation.destinations.single { it.section == GalleryBrowseSection.ALBUMS }.selected)
        assertEquals(1, navigation.destinations.single { it.section == GalleryBrowseSection.PICTURES }.itemCount)
    }

    @Test
    fun browseSectionsCycleDeterministically() {
        assertEquals(GalleryBrowseSection.ALBUMS, GalleryBrowseSection.PICTURES.next())
        assertEquals(GalleryBrowseSection.PICTURES, GalleryBrowseSection.ALBUMS.next())
    }

    private fun media(
        id: String,
        albumName: String?,
        albumId: String?,
        minute: Long,
    ) = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        capturedAt = null,
        modifiedAt = Instant.parse("2026-08-31T12:${minute.toString().padStart(2, '0')}:00Z"),
        width = 1920,
        height = 1080,
        durationMillis = null,
        sizeBytes = 1024,
        albumId = albumId,
        albumName = albumName,
    )
}
