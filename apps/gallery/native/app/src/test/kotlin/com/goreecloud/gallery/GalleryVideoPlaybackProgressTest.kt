package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GalleryVideoPlaybackProgressTest {
    @Test
    fun `known duration clamps seeks and observed positions`() {
        var progress = GalleryVideoPlaybackProgress.initial(videoPlan(durationMillis = 10_000))
        assertEquals(0, progress.positionMillis)
        assertEquals(10_000, progress.durationMillis)
        assertEquals(10_000, progress.seekTarget(12_000))

        progress = progress.seek(7_500)
        assertEquals(7_500, progress.positionMillis)
        progress = progress.observe(12_000)
        assertEquals(10_000, progress.positionMillis)
        assertEquals(10_000, progress.completed().positionMillis)
    }

    @Test
    fun `unknown duration can adopt observed duration`() {
        var progress = GalleryVideoPlaybackProgress.initial(videoPlan(durationMillis = null))
        progress = progress.observe(observedPositionMillis = 2_000, observedDurationMillis = 8_000)
        assertEquals(2_000, progress.positionMillis)
        assertEquals(8_000, progress.durationMillis)
        assertEquals(8_000, progress.seekTarget(9_000))
    }

    @Test
    fun `platform seek bound is enforced even for longer metadata`() {
        val progress = GalleryVideoPlaybackProgress.initial(
            videoPlan(durationMillis = GalleryVideoPlaybackProgress.MAX_PLATFORM_SEEK_MILLIS + 10_000),
        )
        assertEquals(
            GalleryVideoPlaybackProgress.MAX_PLATFORM_SEEK_MILLIS,
            progress.seekTarget(Long.MAX_VALUE),
        )
    }

    @Test
    fun `negative positions durations and image plans are rejected`() {
        val progress = GalleryVideoPlaybackProgress.initial(videoPlan(durationMillis = 10_000))
        assertFailsWith<IllegalArgumentException> { progress.seekTarget(-1) }
        assertFailsWith<IllegalArgumentException> { progress.observe(-1) }
        assertFailsWith<IllegalArgumentException> { progress.observe(0, -1) }
        assertFailsWith<IllegalArgumentException> {
            GalleryVideoPlaybackProgress.initial(
                GalleryViewerPlaybackPlan(
                    presentation = GalleryViewerPresentation.STATIC_IMAGE,
                    shouldAutoPlay = false,
                    shouldLoop = false,
                    durationMillis = null,
                ),
            )
        }
    }

    private fun videoPlan(durationMillis: Long?) = GalleryViewerPlaybackPlan(
        presentation = GalleryViewerPresentation.VIDEO_PLAYBACK,
        shouldAutoPlay = false,
        shouldLoop = false,
        durationMillis = durationMillis,
    )
}
