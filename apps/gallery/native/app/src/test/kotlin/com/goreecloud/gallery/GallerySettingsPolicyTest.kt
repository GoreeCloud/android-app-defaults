package com.goreecloud.gallery

import com.goreecloud.gallery.core.MediaItem
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GallerySettingsPolicyTest {
    @Test
    fun `fast loading remains default and provides higher thumbnail concurrency`() {
        val settings = GalleryUserSettings()
        assertEquals(GalleryFileLoadingPriority.FAST, settings.fileLoadingPriority)
        assertTrue(GalleryFileLoadingPriority.FAST.thumbnailWorkerCount > GalleryFileLoadingPriority.SLOW.thumbnailWorkerCount)
        assertEquals(GalleryFileLoadingPriority.FAST, GalleryFileLoadingPriority.fromStored("unexpected"))
    }

    @Test
    fun `spacious density reduces columns without dropping below two`() {
        listOf(320, 600, 840).forEach { widthDp ->
            val dense = GalleryViewDensity.DENSE.mediaGridColumns(widthDp)
            val comfortable = GalleryViewDensity.COMFORTABLE.mediaGridColumns(widthDp)
            val spacious = GalleryViewDensity.SPACIOUS.mediaGridColumns(widthDp)

            assertTrue(dense >= comfortable)
            assertTrue(comfortable >= spacious)
            assertTrue(spacious >= 2)
            assertEquals((dense - 1).coerceAtLeast(2), comfortable)
            assertEquals((dense - 2).coerceAtLeast(2), spacious)
        }
        assertEquals(GalleryViewDensity.DENSE, GalleryViewDensity.fromStored("future-value"))
    }

    @Test
    fun `included folders narrow the current authorized snapshot`() {
        val visible = GallerySettingsPolicy.visibleItems(
            items = listOf(item("camera", "Camera"), item("download", "Download"), ungroupedItem()),
            settings = GalleryUserSettings(includedAlbumIds = setOf("camera")),
        )
        assertEquals(listOf("camera-item"), visible.map { it.id })
    }

    @Test
    fun `excluded folders win over included folders`() {
        val visible = GallerySettingsPolicy.visibleItems(
            items = listOf(item("camera", "Camera"), item("download", "Download")),
            settings = GalleryUserSettings(
                includedAlbumIds = setOf("camera", "download"),
                excludedAlbumIds = setOf("download"),
            ),
        )
        assertEquals(listOf("camera-item"), visible.map { it.id })
    }

    @Test
    fun `hidden items are suppressed by default without expanding media authority`() {
        val hidden = item("hidden", ".Private", displayName = ".secret.jpg")
        assertTrue(GallerySettingsPolicy.isHidden(hidden))
        assertFalse(GalleryUserSettings().showHiddenItems)
        assertTrue(GallerySettingsPolicy.visibleItems(listOf(hidden), GalleryUserSettings()).isEmpty())
        assertEquals(
            listOf(hidden),
            GallerySettingsPolicy.visibleItems(listOf(hidden), GalleryUserSettings(showHiddenItems = true)),
        )
    }

    @Test
    fun `recycle bin and rounded square defaults are conservative`() {
        val settings = GalleryUserSettings()
        assertTrue(settings.moveDeletedItemsToRecycleBin)
        assertTrue(settings.roundedSquareThumbnails)
        assertFalse(settings.deleteEmptyFolders)
        assertFalse(settings.playVideosAutomatically)
        assertFalse(settings.loopVideos)
    }

    private fun item(albumId: String, albumName: String, displayName: String = "$albumId.jpg") = MediaItem(
        id = "$albumId-item",
        contentUri = "content://gallery/$albumId",
        displayName = displayName,
        mimeType = "image/jpeg",
        capturedAt = null,
        modifiedAt = Instant.EPOCH,
        width = 100,
        height = 100,
        durationMillis = null,
        sizeBytes = 100,
        albumId = albumId,
        albumName = albumName,
    )

    private fun ungroupedItem() = MediaItem(
        id = "ungrouped",
        contentUri = "content://gallery/ungrouped",
        displayName = "ungrouped.jpg",
        mimeType = "image/jpeg",
        capturedAt = null,
        modifiedAt = Instant.EPOCH,
        width = 100,
        height = 100,
        durationMillis = null,
        sizeBytes = 100,
    )
}
