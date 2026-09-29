package com.goreecloud.gallery

enum class GalleryVideoPlaybackState {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    HOST_PAUSED,
    COMPLETED,
    ERROR,
}

/**
 * Pure lifecycle state for one local Gallery video presentation. It tracks
 * playback intent separately from Android VideoView so host pause/resume and
 * asynchronous preparation cannot accidentally start background playback.
 */
class GalleryVideoPlaybackSession private constructor(
    val state: GalleryVideoPlaybackState,
    private val plan: GalleryViewerPlaybackPlan?,
    private val playWhenReady: Boolean,
    private val resumeAfterHostPause: Boolean,
) {
    fun wantsPlayback(): Boolean =
        state == GalleryVideoPlaybackState.PLAYING ||
            (state == GalleryVideoPlaybackState.LOADING && playWhenReady)

    fun prepared(): GalleryVideoPlaybackSession {
        if (state != GalleryVideoPlaybackState.LOADING) return this
        return copy(
            state = if (playWhenReady) GalleryVideoPlaybackState.PLAYING else GalleryVideoPlaybackState.READY,
            playWhenReady = false,
        )
    }

    fun play(): GalleryVideoPlaybackSession = when (state) {
        GalleryVideoPlaybackState.LOADING -> copy(
            playWhenReady = true,
            resumeAfterHostPause = false,
        )
        GalleryVideoPlaybackState.READY,
        GalleryVideoPlaybackState.PAUSED,
        GalleryVideoPlaybackState.COMPLETED,
        -> copy(
            state = GalleryVideoPlaybackState.PLAYING,
            playWhenReady = false,
            resumeAfterHostPause = false,
        )
        GalleryVideoPlaybackState.PLAYING -> this
        GalleryVideoPlaybackState.IDLE,
        GalleryVideoPlaybackState.HOST_PAUSED,
        GalleryVideoPlaybackState.ERROR,
        -> this
    }

    fun pause(): GalleryVideoPlaybackSession = when (state) {
        GalleryVideoPlaybackState.LOADING -> copy(
            playWhenReady = false,
            resumeAfterHostPause = false,
        )
        GalleryVideoPlaybackState.PLAYING,
        GalleryVideoPlaybackState.HOST_PAUSED,
        -> copy(
            state = GalleryVideoPlaybackState.PAUSED,
            playWhenReady = false,
            resumeAfterHostPause = false,
        )
        else -> this
    }

    fun pauseForHost(): GalleryVideoPlaybackSession = when {
        state == GalleryVideoPlaybackState.LOADING && playWhenReady -> copy(
            playWhenReady = false,
            resumeAfterHostPause = true,
        )
        state == GalleryVideoPlaybackState.PLAYING -> copy(
            state = GalleryVideoPlaybackState.HOST_PAUSED,
            playWhenReady = false,
            resumeAfterHostPause = true,
        )
        else -> this
    }

    fun resumeForHost(): GalleryVideoPlaybackSession = when {
        state == GalleryVideoPlaybackState.LOADING && resumeAfterHostPause -> copy(
            playWhenReady = true,
            resumeAfterHostPause = false,
        )
        state == GalleryVideoPlaybackState.READY && resumeAfterHostPause -> copy(
            state = GalleryVideoPlaybackState.PLAYING,
            resumeAfterHostPause = false,
        )
        state == GalleryVideoPlaybackState.HOST_PAUSED && resumeAfterHostPause -> copy(
            state = GalleryVideoPlaybackState.PLAYING,
            resumeAfterHostPause = false,
        )
        else -> this
    }

    fun completed(): GalleryVideoPlaybackSession {
        if (state != GalleryVideoPlaybackState.PLAYING) return this
        return if (plan?.shouldLoop == true) this else copy(state = GalleryVideoPlaybackState.COMPLETED)
    }

    fun failed(): GalleryVideoPlaybackSession =
        if (state == GalleryVideoPlaybackState.IDLE) this
        else copy(
            state = GalleryVideoPlaybackState.ERROR,
            playWhenReady = false,
            resumeAfterHostPause = false,
        )

    fun stopped(): GalleryVideoPlaybackSession = idle()

    private fun copy(
        state: GalleryVideoPlaybackState = this.state,
        plan: GalleryViewerPlaybackPlan? = this.plan,
        playWhenReady: Boolean = this.playWhenReady,
        resumeAfterHostPause: Boolean = this.resumeAfterHostPause,
    ): GalleryVideoPlaybackSession = GalleryVideoPlaybackSession(
        state = state,
        plan = plan,
        playWhenReady = playWhenReady,
        resumeAfterHostPause = resumeAfterHostPause,
    )

    companion object {
        fun idle(): GalleryVideoPlaybackSession = GalleryVideoPlaybackSession(
            state = GalleryVideoPlaybackState.IDLE,
            plan = null,
            playWhenReady = false,
            resumeAfterHostPause = false,
        )

        fun loading(plan: GalleryViewerPlaybackPlan): GalleryVideoPlaybackSession {
            require(plan.presentation == GalleryViewerPresentation.VIDEO_PLAYBACK) {
                "video playback session requires a video playback plan"
            }
            return GalleryVideoPlaybackSession(
                state = GalleryVideoPlaybackState.LOADING,
                plan = plan,
                playWhenReady = plan.shouldAutoPlay,
                resumeAfterHostPause = false,
            )
        }
    }
}
