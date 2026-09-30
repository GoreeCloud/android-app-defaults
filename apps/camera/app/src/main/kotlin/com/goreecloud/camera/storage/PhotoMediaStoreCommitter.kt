// File internal version: 0.1.0
package com.goreecloud.camera.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore

data class PendingPhoto(
    val uri: Uri,
    val displayName: String,
)

class PhotoMediaStoreCommitter(
    private val contentResolver: ContentResolver,
) {
    fun reserve(epochMillis: Long = System.currentTimeMillis()): PendingPhoto {
        val displayName = PhotoFileNamer.displayName(epochMillis)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, JPEG_MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, "$MEDIA_RELATIVE_PATH/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(IMAGE_COLLECTION, values)
            ?: throw IllegalStateException("MediaStore did not reserve an image destination")

        return PendingPhoto(uri = uri, displayName = displayName)
    }

    fun commit(pendingPhoto: PendingPhoto, jpegBytes: ByteArray): Uri {
        require(looksLikeJpeg(jpegBytes)) { "Camera output is not a JPEG image" }

        try {
            val outputStream = contentResolver.openOutputStream(pendingPhoto.uri, "w")
                ?: throw IllegalStateException("Unable to open the pending MediaStore image")

            outputStream.use { stream ->
                stream.write(jpegBytes)
                stream.flush()
            }

            val published = contentResolver.update(
                pendingPhoto.uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
            if (published <= 0) {
                throw IllegalStateException("MediaStore did not publish the completed image")
            }

            return pendingPhoto.uri
        } catch (exception: Exception) {
            discard(pendingPhoto)
            throw exception
        }
    }

    fun discard(pendingPhoto: PendingPhoto) {
        runCatching { contentResolver.delete(pendingPhoto.uri, null, null) }
    }

    internal fun looksLikeJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 2 &&
            bytes[0].toInt() and 0xFF == 0xFF &&
            bytes[1].toInt() and 0xFF == 0xD8

    private companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val MEDIA_RELATIVE_PATH = "DCIM/GoreeCloud Camera"
        val IMAGE_COLLECTION: Uri =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }
}
