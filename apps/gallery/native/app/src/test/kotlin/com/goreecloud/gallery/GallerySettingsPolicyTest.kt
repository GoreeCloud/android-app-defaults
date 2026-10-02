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
    fun `quick presentation controls cycle deterministically and wrap`() {
        assertEquals(GalleryGroupingMode.MONTH, GalleryGroupingMode.DAY.next())
        assertEquals(GalleryGroupingMode.YEAR, GalleryGroupingMode.MONTH.next())
        assertEquals(GalleryGroupingMode.NONE, GalleryGroupingMode.YEAR.next())
        assertEquals(GalleryGroupingMode.DAY, GalleryGroupingMode.NONE.next())

        assertEquals(GalleryViewDensity.COMFORTABLE, GalleryViewDensity.DENSE.next())
        assertEquals(GalleryViewDensity.SPACIOUS, GalleryViewDensity.COMFORTABLE.next())
        assertEquals(GalleryViewDensity.DENSE, GalleryViewDensity.SPACIOUS.next())
    }

    @Test
    fun `manual album order is opt in and empty by default`() {
        assertTrue(GalleryUserSettings().albumOrderIds.isEmpty())
    }

    @Test
    fun `slideshow interval defaults to five seconds and restores portable values`() {
        assertEquals(GallerySlideshowInterval.NORMAL, GalleryUserSettings().slideshowInterval)
        assertEquals(5_000L, GallerySlideshowInterval.NORMAL.intervalMs)
        assertEquals(GallerySlideshowInterval.FAST, GallerySlideshowInterval.fromStored("fast"))
        assertEquals(GallerySlideshowInterval.RELAXED, GallerySlideshowInterval.fromStored("relaxed"))
        assertEquals(GallerySlideshowInterval.NORMAL, GallerySlideshowInterval.fromStored("future-value"))
    }

    @Test
    fun `video presentation uses mockup-aligned recency wording`() {
        assertEquals(
            "Recently added",
            GalleryVideoPresentationPolicy.filterAndOrderLabel(
                GalleryVideoFilter.ALL,
                com.goreecloud.gallery.core.MediaSortOrder.NEWEST,
            ),
        )
        assertEquals(
            "Screen recordings · Recently added",
            GalleryVideoPresentationPolicy.filterAndOrderLabel(
                GalleryVideoFilter.SCREEN_RECORDINGS,
                com.goreecloud.gallery.core.MediaSortOrder.NEWEST,
            ),
        )
        assertEquals(
            "Oldest first",
            GalleryVideoPresentationPolicy.filterAndOrderLabel(
                GalleryVideoFilter.ALL,
                com.goreecloud.gallery.core.MediaSortOrder.OLDEST,
            ),
        )
    }

    @Test
    fun `video filters expose only categories present in the authorized snapshot`() {
        val camera = video("camera", "Camera", "VID_001.mp4")
        val recording = video("screens", "Screen recordings", "screen_recording_001.mp4")
        val favorite = video("movies", "Movies", "clip.mp4")

        assertEquals(
            listOf(
                GalleryVideoFilter.ALL,
                GalleryVideoFilter.SCREEN_RECORDINGS,
                GalleryVideoFilter.CAMERA,
                GalleryVideoFilter.FAVORITES,
            ),
            GalleryVideoFilterPolicy.available(
                listOf(camera, recording, favorite),
                favoriteContentUris = setOf(favorite.contentUri),
            ),
        )
        assertEquals(
            listOf(camera),
            GalleryVideoFilterPolicy.filter(
                listOf(camera, recording, favorite),
                GalleryVideoFilter.CAMERA,
                favoriteContentUris = emptySet(),
            ),
        )
        assertEquals(
            listOf(recording),
            GalleryVideoFilterPolicy.filter(
                listOf(camera, recording, favorite),
                GalleryVideoFilter.SCREEN_RECORDINGS,
                favoriteContentUris = emptySet(),
            ),
        )
        assertEquals(
            listOf(favorite),
            GalleryVideoFilterPolicy.filter(
                listOf(camera, recording, favorite),
                GalleryVideoFilter.FAVORITES,
                favoriteContentUris = setOf(favorite.contentUri),
            ),
        )
    }

    @Test
    fun `sparse dense groups use a three-column presentation lane`() {
        assertEquals(3, GalleryViewDensity.DENSE.mediaGridColumnsForGroup(360, 1))
        assertEquals(3, GalleryViewDensity.DENSE.mediaGridColumnsForGroup(360, 2))
        assertEquals(4, GalleryViewDensity.DENSE.mediaGridColumnsForGroup(360, 4))
        assertEquals(
            GalleryViewDensity.COMFORTABLE.mediaGridColumns(360),
            GalleryViewDensity.COMFORTABLE.mediaGridColumnsForGroup(360, 1),
        )
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

    private fun video(id: String, albumName: String, displayName: String) = MediaItem(
        id = id,
        contentUri = "content://gallery/$id",
        displayName = displayName,
        mimeType = "video/mp4",
        capturedAt = Instant.parse("2026-09-29T12:00:00Z"),
        modifiedAt = Instant.parse("2026-09-29T12:00:00Z"),
        width = 1920,
        height = 1080,
        durationMillis = 120_000,
        sizeBytes = 1_000,
        albumId = albumName.lowercase().replace(" ", "-"),
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
