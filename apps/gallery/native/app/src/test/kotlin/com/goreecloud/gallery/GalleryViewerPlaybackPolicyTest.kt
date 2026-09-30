package com.goreecloud.gallery

import com.goreecloud.gallery.core.MediaItem
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryViewerPlaybackPolicyTest {
    @Test
    fun `photos always use static presentation and ignore video playback preferences`() {
        val plan = GalleryViewerPlaybackPolicy.plan(
            item = item(mimeType = "image/jpeg", durationMillis = null),
            settings = GalleryUserSettings(
                playVideosAutomatically = true,
                loopVideos = true,
            ),
        )

        assertEquals(GalleryViewerPresentation.STATIC_IMAGE, plan.presentation)
        assertFalse(plan.shouldAutoPlay)
        assertFalse(plan.shouldLoop)
        assertNull(plan.durationMillis)
    }

    @Test
    fun `videos inherit autoplay and loop preferences`() {
        val plan = GalleryViewerPlaybackPolicy.plan(
            item = item(mimeType = "video/mp4", durationMillis = 42_000),
            settings = GalleryUserSettings(
                playVideosAutomatically = true,
                loopVideos = true,
            ),
        )

        assertEquals(GalleryViewerPresentation.VIDEO_PLAYBACK, plan.presentation)
        assertTrue(plan.shouldAutoPlay)
        assertTrue(plan.shouldLoop)
        assertEquals(42_000, plan.durationMillis)
    }

    @Test
    fun `video defaults remain paused and non looping`() {
        val plan = GalleryViewerPlaybackPolicy.plan(
            item = item(mimeType = "video/webm", durationMillis = null),
            settings = GalleryUserSettings(),
        )

        assertEquals(GalleryViewerPresentation.VIDEO_PLAYBACK, plan.presentation)
        assertFalse(plan.shouldAutoPlay)
        assertFalse(plan.shouldLoop)
        assertNull(plan.durationMillis)
    }

    private fun item(mimeType: String, durationMillis: Long?) = MediaItem(
        id = "media-1",
        contentUri = "content://media/external/video/media/1",
        displayName = if (mimeType.startsWith("video/")) "clip.mp4" else "photo.jpg",
        mimeType = mimeType,
        capturedAt = null,
        modifiedAt = Instant.EPOCH,
        width = 1920,
        height = 1080,
        durationMillis = durationMillis,
        sizeBytes = 1_024,
    )
}
