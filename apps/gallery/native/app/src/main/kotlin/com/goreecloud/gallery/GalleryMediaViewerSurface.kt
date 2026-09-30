package com.goreecloud.gallery

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.goreecloud.gallery.android.AndroidMediaStoreItemUriPolicy
import com.goreecloud.gallery.core.MediaItem
import com.goreecloud.gallery.core.MediaKind

/**
 * Unified programmatic local-media viewer surface for an already-authorized
 * GoreeCloud Gallery MediaStore item.
 *
 * Images are presented through Android ImageView and videos through the
 * validated GalleryVideoPlayerSurface. Both paths require the shared canonical
 * MediaStore item-URI boundary, so this surface never accepts file paths, HTTP
 * URLs, collection URIs, or arbitrary ContentProviders.
 *
 * The surface remains intentionally independent from GalleryActivity so it can
 * be compiled and validated without widening Activity lifecycle/state scope.
 */
internal class GalleryMediaViewerSurface(
    context: Context,
) : FrameLayout(context) {
    private val imageView = ImageView(context).apply {
        setBackgroundColor(Color.BLACK)
        scaleType = ImageView.ScaleType.FIT_CENTER
        visibility = View.GONE
    }
    private val videoSurface = GalleryVideoPlayerSurface(context).apply {
        visibility = View.GONE
    }

    private var currentItemId: String? = null
    private var currentKind: MediaKind? = null

    var onVideoPlaybackError: ((what: Int, extra: Int) -> Unit)?
        get() = videoSurface.onPlaybackError
        set(value) {
            videoSurface.onPlaybackError = value
        }

    init {
        setBackgroundColor(Color.BLACK)
        addView(
            imageView,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            videoSurface,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun show(item: MediaItem, settings: GalleryUserSettings) {
        val canonicalUri = AndroidMediaStoreItemUriPolicy.requireCanonicalItemUri(item.contentUri)
        val plan = GalleryViewerPlaybackPolicy.plan(item, settings)

        currentItemId = item.id
        currentKind = item.kind

        when (plan.presentation) {
            GalleryViewerPresentation.STATIC_IMAGE -> {
                videoSurface.stop()
                videoSurface.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(Uri.parse(canonicalUri))
            }

            GalleryViewerPresentation.VIDEO_PLAYBACK -> {
                imageView.setImageDrawable(null)
                imageView.visibility = View.GONE
                videoSurface.visibility = View.VISIBLE
                videoSurface.load(canonicalUri, plan)
            }
        }
    }

    fun clear() {
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
        videoSurface.stop()
        videoSurface.visibility = View.GONE
        currentItemId = null
        currentKind = null
    }

    fun playVideo(): Boolean =
        currentKind == MediaKind.VIDEO && videoSurface.play()

    fun pauseVideo(): Boolean =
        currentKind == MediaKind.VIDEO && videoSurface.pause()

    fun seekVideoTo(positionMillis: Long): Boolean =
        currentKind == MediaKind.VIDEO && videoSurface.seekTo(positionMillis)

    fun pauseForHost(): Boolean =
        currentKind == MediaKind.VIDEO && videoSurface.pauseForHost()

    fun resumeForHost(): Boolean =
        currentKind == MediaKind.VIDEO && videoSurface.resumeForHost()

    fun currentItemId(): String? = currentItemId

    fun currentKind(): MediaKind? = currentKind

    fun videoPlaybackState(): GalleryVideoPlaybackState? =
        if (currentKind == MediaKind.VIDEO) videoSurface.playbackState() else null

    fun videoProgressSnapshot(): GalleryVideoPlaybackProgress? =
        if (currentKind == MediaKind.VIDEO) videoSurface.progressSnapshot() else null
}
