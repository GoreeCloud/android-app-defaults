package com.goreecloud.gallery

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Narrow external media viewer used when Android routes an image/video VIEW or REVIEW intent
 * to GoreeCloud Gallery.
 *
 * This Activity does not enumerate the media library and does not widen storage authority.
 * It renders only the URI supplied by the caller and only for content/file image or video media.
 */
class GalleryExternalMediaActivity : Activity() {
    private lateinit var root: FrameLayout
    private var videoView: VideoView? = null
    private var animatedDrawable: Animatable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildSurface()
        renderIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        setIntent(intent)
        renderIntent(intent)
    }

    override fun onPause() {
        videoView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        animatedDrawable?.stop()
        animatedDrawable = null
        videoView?.stopPlayback()
        videoView = null
        super.onDestroy()
    }

    private fun buildSurface() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(root)

        val close = ImageView(this).apply {
            setImageResource(R.drawable.ic_gallery_back)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            contentDescription = "Close media viewer"
            isClickable = true
            isFocusable = true
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { finish() }
        }
        root.addView(
            close,
            FrameLayout.LayoutParams(dp(56), dp(56)).apply {
                gravity = Gravity.START or Gravity.TOP
                marginStart = dp(8)
                topMargin = dp(8)
            },
        )
    }

    private fun renderIntent(intent: Intent) {
        animatedDrawable?.stop()
        animatedDrawable = null
        videoView?.stopPlayback()
        videoView = null
        removeMediaSurface()

        if (GalleryExternalMediaIntentPolicy.isSecureReview(intent.action)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val uri = intent.data
        if (uri == null) {
            renderFailure("This media request did not include an item to open.")
            return
        }

        val mimeType = resolveMimeType(intent, uri)
        if (!GalleryExternalMediaIntentPolicy.accepts(intent.action, uri.scheme, mimeType)) {
            renderFailure("Gallery can open only local image and video media supplied by Android.")
            return
        }

        when (GalleryExternalMediaIntentPolicy.kindFor(mimeType)) {
            GalleryExternalMediaIntentPolicy.Kind.IMAGE -> renderImage(uri)
            GalleryExternalMediaIntentPolicy.Kind.VIDEO -> renderVideo(uri)
            null -> renderFailure("Gallery could not determine this media type.")
        }
    }

    private fun resolveMimeType(intent: Intent, uri: Uri): String? {
        intent.type?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            contentResolver.getType(uri)
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun renderImage(uri: Uri) {
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            contentDescription = "Image opened in GoreeCloud Gallery"
        }

        val drawable = try {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                val targetEdge = max(
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels,
                ).coerceAtLeast(1) * 2
                val sourceEdge = max(info.size.width, info.size.height)
                if (sourceEdge > targetEdge && info.size.width > 0 && info.size.height > 0) {
                    val scale = targetEdge.toFloat() / sourceEdge.toFloat()
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        } catch (_: Exception) {
            null
        }

        if (drawable == null) {
            renderFailure("Android did not grant Gallery readable access to this image.")
            return
        }

        image.setImageDrawable(drawable)
        animatedDrawable = drawable as? Animatable
        animatedDrawable?.start()
        addMediaSurface(image)
    }

    private fun renderVideo(uri: Uri) {
        val video = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            contentDescription = "Video opened in GoreeCloud Gallery"
        }
        val controls = MediaController(this).apply {
            setAnchorView(video)
        }
        video.setMediaController(controls)
        video.setOnPreparedListener {
            video.seekTo(1)
            controls.show(3_000)
        }
        video.setOnErrorListener { _, _, _ ->
            renderFailure("Android could not play this video in Gallery.")
            true
        }

        try {
            video.setVideoURI(uri)
            video.requestFocus()
        } catch (_: SecurityException) {
            renderFailure("Android did not grant Gallery readable access to this video.")
            return
        } catch (_: RuntimeException) {
            renderFailure("Android could not prepare this video for Gallery.")
            return
        }

        videoView = video
        addMediaSurface(video)
    }

    private fun addMediaSurface(view: android.view.View) {
        root.addView(
            view,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = dp(72)
                bottomMargin = dp(24)
                marginStart = dp(12)
                marginEnd = dp(12)
            },
        )
    }

    private fun removeMediaSurface() {
        while (root.childCount > 1) {
            root.removeViewAt(0)
        }
    }

    private fun renderFailure(message: String) {
        animatedDrawable?.stop()
        animatedDrawable = null
        videoView?.stopPlayback()
        videoView = null
        removeMediaSurface()

        val text = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            this.text = message
            contentDescription = message
        }
        addMediaSurface(text)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics,
        ).roundToInt()
}
