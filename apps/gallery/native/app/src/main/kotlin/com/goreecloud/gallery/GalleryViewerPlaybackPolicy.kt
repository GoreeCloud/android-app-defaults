package com.goreecloud.gallery

import com.goreecloud.gallery.core.MediaItem
import com.goreecloud.gallery.core.MediaKind

enum class GalleryViewerPresentation {
    STATIC_IMAGE,
    VIDEO_PLAYBACK,
}

data class GalleryViewerPlaybackPlan(
    val presentation: GalleryViewerPresentation,
    val shouldAutoPlay: Boolean,
    val shouldLoop: Boolean,
    val durationMillis: Long?,
)

/**
 * Pure viewer policy for mapping an already-authorized local MediaItem and the
 * current Gallery settings into presentation/playback intent.
 *
 * This policy does not instantiate a player, decode media, open content URIs,
 * request permissions, or expand Android MediaStore authority.
 */
object GalleryViewerPlaybackPolicy {
    fun plan(item: MediaItem, settings: GalleryUserSettings): GalleryViewerPlaybackPlan =
        when (item.kind) {
            MediaKind.IMAGE -> GalleryViewerPlaybackPlan(
                presentation = GalleryViewerPresentation.STATIC_IMAGE,
                shouldAutoPlay = false,
                shouldLoop = false,
                durationMillis = null,
            )
            MediaKind.VIDEO -> GalleryViewerPlaybackPlan(
                presentation = GalleryViewerPresentation.VIDEO_PLAYBACK,
                shouldAutoPlay = settings.playVideosAutomatically,
                shouldLoop = settings.loopVideos,
                durationMillis = item.durationMillis,
            )
        }
}
