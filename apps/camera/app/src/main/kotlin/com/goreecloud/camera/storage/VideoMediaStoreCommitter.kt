// File internal version: 0.2.0
package com.goreecloud.camera.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.system.Os

data class PendingVideo(
    val uri: Uri,
    val displayName: String,
    val fileDescriptor: ParcelFileDescriptor,
)

class VideoMediaStoreCommitter(
    private val contentResolver: ContentResolver,
) {
    fun reserve(epochMillis: Long = System.currentTimeMillis()): PendingVideo {
        val displayName = VideoFileNamer.displayName(epochMillis)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, MP4_MIME_TYPE)
            put(MediaStore.Video.Media.RELATIVE_PATH, "$MEDIA_RELATIVE_PATH/")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(VIDEO_COLLECTION, values)
            ?: throw IllegalStateException("MediaStore did not reserve a video destination")

        val descriptor = try {
            contentResolver.openFileDescriptor(uri, "rw")
                ?: throw IllegalStateException("Unable to open the pending MediaStore video")
        } catch (exception: Exception) {
            runCatching { contentResolver.delete(uri, null, null) }
            throw exception
        }

        return PendingVideo(uri = uri, displayName = displayName, fileDescriptor = descriptor)
    }

    fun publish(pendingVideo: PendingVideo): Uri {
        try {
            // MediaStore SIZE metadata can remain stale while a row is still pending.
            // Validate the actual recorder destination before closing it or clearing IS_PENDING.
            val recordedSize = Os.fstat(pendingVideo.fileDescriptor.fileDescriptor).st_size
            if (recordedSize <= 0L) {
                throw IllegalStateException("Recorded video is empty")
            }

            pendingVideo.fileDescriptor.close()

            val published = contentResolver.update(
                pendingVideo.uri,
                ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) },
                null,
                null,
            )
            if (published <= 0) {
                throw IllegalStateException("MediaStore did not publish the completed video")
            }

            return pendingVideo.uri
        } catch (exception: Exception) {
            discard(pendingVideo)
            throw exception
        }
    }

    fun discard(pendingVideo: PendingVideo) {
        runCatching { pendingVideo.fileDescriptor.close() }
        runCatching { contentResolver.delete(pendingVideo.uri, null, null) }
    }

    private companion object {
        const val MP4_MIME_TYPE = "video/mp4"
        const val MEDIA_RELATIVE_PATH = "DCIM/GoreeCloud Camera"
        val VIDEO_COLLECTION: Uri =
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }
}
