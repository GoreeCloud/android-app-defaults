package com.goreecloud.gallery

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.ExifInterface
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryViewerOrientationRuntimeTest {
    @Test
    fun boundedViewerLoaderHonorsEncodedNinetyDegreeOrientation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val uri = insertJpeg(
            displayName = "gallery-viewer-orientation-90.jpg",
            orientation = ExifInterface.ORIENTATION_ROTATE_90,
        )

        try {
            val decoded = GalleryViewerBitmapLoader.load(
                contentResolver = resolver,
                contentUri = uri,
                mimeType = "image/jpeg",
                viewportWidth = 100,
                viewportHeight = 100,
                fallbackThumbnailPx = 64,
            )

            assertNotNull("Orientation-aware viewer decode must return a bitmap", decoded)
            val bitmap = checkNotNull(decoded)
            try {
                assertEquals(
                    "ImageDecoder must normalize the encoded 90-degree orientation",
                    2,
                    bitmap.width,
                )
                assertEquals(
                    "ImageDecoder must normalize the encoded 90-degree orientation",
                    4,
                    bitmap.height,
                )
            } finally {
                bitmap.recycle()
            }
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    @Test
    fun boundedViewerLoaderDoesNotRotateNormallyOrientedImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val uri = insertJpeg(
            displayName = "gallery-viewer-orientation-normal.jpg",
            orientation = ExifInterface.ORIENTATION_NORMAL,
        )

        try {
            val decoded = GalleryViewerBitmapLoader.load(
                contentResolver = resolver,
                contentUri = uri,
                mimeType = "image/jpeg",
                viewportWidth = 100,
                viewportHeight = 100,
                fallbackThumbnailPx = 64,
            )

            assertNotNull("Normally oriented viewer decode must return a bitmap", decoded)
            val bitmap = checkNotNull(decoded)
            try {
                assertEquals(4, bitmap.width)
                assertEquals(2, bitmap.height)
            } finally {
                bitmap.recycle()
            }
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    private fun insertJpeg(
        displayName: String,
        orientation: Int,
    ): android.net.Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val collection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(
            resolver.insert(
                collection,
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/GoreeCloud Gallery Tests",
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                },
            ),
        )

        try {
            resolver.openOutputStream(uri, "w").use { output ->
                checkNotNull(output)
                val bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
                try {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output))
                } finally {
                    bitmap.recycle()
                }
            }

            resolver.openFileDescriptor(uri, "rw").use { descriptor ->
                checkNotNull(descriptor)
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
            }

            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null,
            )
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
