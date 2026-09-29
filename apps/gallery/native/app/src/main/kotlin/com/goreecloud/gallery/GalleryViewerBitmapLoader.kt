package com.goreecloud.gallery

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import java.io.IOException

/**
 * Bounded bitmap loader for the authorized full-screen Gallery viewer.
 *
 * The caller remains responsible for proving that [contentUri] belongs to the current authorized
 * Gallery scope. Images prefer the orientation-aware decoder when the viewer has a measured
 * viewport. Thumbnail loading remains the fallback for decode failure, video posters, unknown
 * media, and pre-layout states.
 */
object GalleryViewerBitmapLoader {
    fun load(
        contentResolver: ContentResolver,
        contentUri: Uri,
        mimeType: String,
        viewportWidth: Int,
        viewportHeight: Int,
        fallbackThumbnailPx: Int,
    ): Bitmap? {
        if (fallbackThumbnailPx <= 0) return null

        if (
            GalleryViewerLoadPolicy.primaryPath(
                mimeType = mimeType,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
            ) == GalleryViewerLoadPath.ORIENTATION_AWARE_IMAGE
        ) {
            GalleryViewerImageDecoder.decodeBounded(
                contentResolver = contentResolver,
                contentUri = contentUri,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
            )?.let { return it }
        }

        return try {
            contentResolver.loadThumbnail(
                contentUri,
                Size(fallbackThumbnailPx, fallbackThumbnailPx),
                null,
            )
        } catch (_: SecurityException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
