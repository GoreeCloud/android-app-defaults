package com.goreecloud.gallery

import android.annotation.TargetApi
import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build

internal object GalleryGifThumbnailLoader {
    fun loadAnimated(
        contentResolver: ContentResolver,
        contentUri: Uri,
        requestedEdgePx: Int,
    ): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || requestedEdgePx <= 0) return null
        return Api28.loadAnimated(contentResolver, contentUri, requestedEdgePx)
    }

    fun startIfAnimated(drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Api28.start(drawable)
    }

    fun stopIfAnimated(drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Api28.stop(drawable)
    }

    @TargetApi(Build.VERSION_CODES.P)
    private object Api28 {
        fun loadAnimated(
            contentResolver: ContentResolver,
            contentUri: Uri,
            requestedEdgePx: Int,
        ): Drawable? {
            val source = ImageDecoder.createSource(contentResolver, contentUri)
            val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                GalleryAnimatedThumbnailPolicy.targetSize(
                    sourceWidth = info.size.width,
                    sourceHeight = info.size.height,
                    requestedEdgePx = requestedEdgePx,
                )?.let { (width, height) ->
                    decoder.setTargetSize(width, height)
                }
            }
            return drawable.takeIf { it is AnimatedImageDrawable }
        }

        fun start(drawable: Drawable?) {
            (drawable as? AnimatedImageDrawable)?.start()
        }

        fun stop(drawable: Drawable?) {
            (drawable as? AnimatedImageDrawable)?.stop()
        }
    }
}
