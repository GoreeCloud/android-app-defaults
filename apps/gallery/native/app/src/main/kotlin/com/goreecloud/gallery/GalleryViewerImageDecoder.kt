package com.goreecloud.gallery

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.IOException

/**
 * Android image decoder for the authorized full-screen Gallery viewer.
 *
 * The caller must supply a content URI that already belongs to the current Android-authorized
 * Gallery presentation scope. ImageDecoder owns encoded-orientation interpretation; this class does
 * not apply an additional EXIF rotation, avoiding a second rotation on providers/decoders that
 * already normalize orientation.
 */
object GalleryViewerImageDecoder {
    fun decodeBounded(
        contentResolver: ContentResolver,
        contentUri: Uri,
        viewportWidth: Int,
        viewportHeight: Int,
    ): Bitmap? = try {
        val source = ImageDecoder.createSource(contentResolver, contentUri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val target = GalleryViewerDecodePolicy.boundedDecodeSize(
                sourceWidth = info.size.width,
                sourceHeight = info.size.height,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
            )
            if (target.width != info.size.width || target.height != info.size.height) {
                decoder.setTargetSize(target.width, target.height)
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: RuntimeException) {
        null
    }
}
