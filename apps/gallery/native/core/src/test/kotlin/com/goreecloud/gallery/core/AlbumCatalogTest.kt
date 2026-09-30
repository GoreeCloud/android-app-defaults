package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AlbumCatalogTest {
    @Test
    fun groupsAuthoritativeAlbumMetadataAndChoosesNewestCover() {
        val items = listOf(
            item("camera-old", 1, "camera", "Camera"),
            item("downloads", 4, "downloads", "Downloads"),
            item("camera-new", 5, "camera", "Camera"),
            item("unfiled", 9, null, null),
        )

        val albums = items.buildAlbumCatalog()

        assertEquals(listOf("camera", "downloads"), albums.map { it.id })
        assertEquals(2, albums[0].itemCount)
        assertEquals("camera-new", albums[0].coverItemId)
        assertEquals(Instant.ofEpochSecond(5), albums[0].newestAt)
        assertEquals(1, albums[1].itemCount)
    }

    @Test
    fun conflictingNamesForOneAuthoritativeAlbumIdFailClosed() {
        val items = listOf(
            item("one", 1, "camera", "Camera"),
            item("two", 2, "camera", "Different name"),
        )

        assertFailsWith<IllegalArgumentException> { items.buildAlbumCatalog() }
    }

    @Test
    fun albumMetadataMustBeComplete() {
        assertFailsWith<IllegalArgumentException> {
            item("invalid", 1, "camera", null)
        }
    }

    private fun item(
        id: String,
        captured: Long,
        albumId: String?,
        albumName: String?,
    ) = MediaItem(
        id = id,
        contentUri = "content://media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        capturedAt = Instant.ofEpochSecond(captured),
        modifiedAt = Instant.ofEpochSecond(captured),
        width = 100,
        height = 100,
        durationMillis = null,
        sizeBytes = 10,
        albumId = albumId,
        albumName = albumName,
    )
}
