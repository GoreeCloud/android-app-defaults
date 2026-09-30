package com.goreecloud.gallery

/**
 * Pure bounded playback-position state for the Android VideoView surface.
 * Positions are non-negative milliseconds. The platform seek API accepts Int,
 * so requested seeks are capped at that boundary even when metadata reports a
 * longer duration.
 */
data class GalleryVideoPlaybackProgress private constructor(
    val positionMillis: Long,
    val durationMillis: Long?,
) {
    init {
        require(positionMillis >= 0)
        require(durationMillis == null || durationMillis >= 0)
        require(durationMillis == null || positionMillis <= durationMillis)
    }

    fun seekTarget(requestedMillis: Long): Long {
        require(requestedMillis >= 0) { "video seek position must be non-negative" }
        val durationBound = durationMillis ?: Long.MAX_VALUE
        return requestedMillis
            .coerceAtMost(durationBound)
            .coerceAtMost(MAX_PLATFORM_SEEK_MILLIS)
    }

    fun seek(requestedMillis: Long): GalleryVideoPlaybackProgress =
        copy(positionMillis = seekTarget(requestedMillis))

    fun observe(
        observedPositionMillis: Long,
        observedDurationMillis: Long? = null,
    ): GalleryVideoPlaybackProgress {
        require(observedPositionMillis >= 0) { "observed video position must be non-negative" }
        require(observedDurationMillis == null || observedDurationMillis >= 0) {
            "observed video duration must be non-negative"
        }
        val duration = observedDurationMillis ?: durationMillis
        val position = if (duration == null) observedPositionMillis else observedPositionMillis.coerceAtMost(duration)
        return GalleryVideoPlaybackProgress(position, duration)
    }

    fun completed(): GalleryVideoPlaybackProgress =
        if (durationMillis == null) this else copy(positionMillis = durationMillis)

    companion object {
        const val MAX_PLATFORM_SEEK_MILLIS: Long = 2147483647L

        fun initial(plan: GalleryViewerPlaybackPlan): GalleryVideoPlaybackProgress {
            require(plan.presentation == GalleryViewerPresentation.VIDEO_PLAYBACK) {
                "video playback progress requires a video playback plan"
            }
            require(plan.durationMillis == null || plan.durationMillis >= 0) {
                "video duration must be non-negative"
            }
            return GalleryVideoPlaybackProgress(
                positionMillis = 0,
                durationMillis = plan.durationMillis,
            )
        }
    }
}
