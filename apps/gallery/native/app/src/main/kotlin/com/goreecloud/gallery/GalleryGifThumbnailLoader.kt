package com.goreecloud.gallery

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build

internal object GalleryGifThumbnailLoader {
    fun loadAnimated(
        contentResolver: ContentResolver,
        contentUri: Uri,
        requestedEdgePx: Int,
    ): AnimatedImageDrawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || requestedEdgePx <= 0) return null

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
        return drawable as? AnimatedImageDrawable
    }
}
