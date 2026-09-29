package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GalleryVideoPlaybackSessionTest {
    @Test
    fun `autoplay intent becomes playing only after preparation`() {
        val autoplay = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = true))
        assertEquals(GalleryVideoPlaybackState.LOADING, autoplay.state)
        assertTrue(autoplay.wantsPlayback())
        assertEquals(GalleryVideoPlaybackState.PLAYING, autoplay.prepared().state)

        val manual = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = false))
        assertFalse(manual.wantsPlayback())
        assertEquals(GalleryVideoPlaybackState.READY, manual.prepared().state)
    }

    @Test
    fun `host pause before preparation defers autoplay until host resumes`() {
        var session = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = true))
        session = session.pauseForHost()
        assertEquals(GalleryVideoPlaybackState.LOADING, session.state)
        assertFalse(session.wantsPlayback())

        session = session.prepared()
        assertEquals(GalleryVideoPlaybackState.READY, session.state)
        assertFalse(session.wantsPlayback())

        session = session.resumeForHost()
        assertEquals(GalleryVideoPlaybackState.PLAYING, session.state)
        assertTrue(session.wantsPlayback())
    }

    @Test
    fun `explicit pause cancels deferred host resume`() {
        var session = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = true))
        session = session.pauseForHost().pause().prepared().resumeForHost()
        assertEquals(GalleryVideoPlaybackState.READY, session.state)
        assertFalse(session.wantsPlayback())
    }

    @Test
    fun `completion respects loop intent`() {
        val oneShot = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = true, loop = false))
            .prepared()
            .completed()
        assertEquals(GalleryVideoPlaybackState.COMPLETED, oneShot.state)
        assertFalse(oneShot.wantsPlayback())

        val looping = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = true, loop = true))
            .prepared()
            .completed()
        assertEquals(GalleryVideoPlaybackState.PLAYING, looping.state)
        assertTrue(looping.wantsPlayback())
    }

    @Test
    fun `failure is terminal until stop and image plans are rejected`() {
        var session = GalleryVideoPlaybackSession.loading(videoPlan(autoPlay = true)).prepared().failed()
        assertEquals(GalleryVideoPlaybackState.ERROR, session.state)
        assertEquals(GalleryVideoPlaybackState.ERROR, session.play().state)
        session = session.stopped()
        assertEquals(GalleryVideoPlaybackState.IDLE, session.state)
        assertFalse(session.wantsPlayback())

        assertFailsWith<IllegalArgumentException> {
            GalleryVideoPlaybackSession.loading(
                GalleryViewerPlaybackPlan(
                    presentation = GalleryViewerPresentation.STATIC_IMAGE,
                    shouldAutoPlay = false,
                    shouldLoop = false,
                    durationMillis = null,
                ),
            )
        }
    }

    private fun videoPlan(
        autoPlay: Boolean,
        loop: Boolean = false,
    ) = GalleryViewerPlaybackPlan(
        presentation = GalleryViewerPresentation.VIDEO_PLAYBACK,
        shouldAutoPlay = autoPlay,
        shouldLoop = loop,
        durationMillis = 10_000,
    )
}
