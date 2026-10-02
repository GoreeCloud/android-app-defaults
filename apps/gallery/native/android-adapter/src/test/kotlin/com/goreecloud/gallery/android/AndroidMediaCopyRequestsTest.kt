package com.goreecloud.gallery.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidMediaCopyRequestsTest {
    @Test
    fun `copy support begins on Android 11`() {
        assertEquals(false, AndroidMediaCopyRequests.isSupported(android.os.Build.VERSION_CODES.Q))
        assertEquals(true, AndroidMediaCopyRequests.isSupported(android.os.Build.VERSION_CODES.R))
    }

    @Test
    fun `copy normalizes exact canonical image and video sources`() {
        val normalized = AndroidMediaCopyRequests.normalizeSources(
            listOf(
                AndroidMediaCopySource(
                    contentUri = "content://media/external/images/media/10",
                    displayName = "IMG_10.JPG",
                    outputDisplayName = "IMG_10 (copy).JPG",
                    mimeType = "IMAGE/JPEG",
                    capturedAtMillis = 10L,
                ),
                AndroidMediaCopySource(
                    contentUri = "content://media/external/video/media/11",
                    displayName = "VID_11.mp4",
                    outputDisplayName = "VID_11 (copy).mp4",
                    mimeType = "video/mp4",
                ),
            ),
        )

        assertEquals(listOf("image/jpeg", "video/mp4"), normalized.map { it.mimeType })
        assertEquals(10L, normalized.first().capturedAtMillis)
    }

    @Test
    fun `copy rejects operations beyond the bounded item limit`() {
        val sources = (1..(AndroidMediaCopyRequests.MAX_COPY_ITEMS + 1)).map { id ->
            AndroidMediaCopySource(
                contentUri = "content://media/external/images/media/$id",
                displayName = "IMG_$id.jpg",
                outputDisplayName = "IMG_$id (copy).jpg",
                mimeType = "image/jpeg",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            AndroidMediaCopyRequests.normalizeSources(sources)
        }
    }

    @Test
    fun `copy rejects duplicate or foreign source URIs`() {
        val image = AndroidMediaCopySource(
            contentUri = "content://media/external/images/media/10",
            displayName = "IMG_10.jpg",
            outputDisplayName = "IMG_10 (copy).jpg",
            mimeType = "image/jpeg",
        )
        assertFailsWith<IllegalArgumentException> {
            AndroidMediaCopyRequests.normalizeSources(listOf(image, image))
        }
        assertFailsWith<IllegalArgumentException> {
            AndroidMediaCopyRequests.normalizeSources(
                listOf(image.copy(contentUri = "content://other/external/images/media/10")),
            )
        }
    }

    @Test
    fun `copy rejects MIME and MediaStore collection mismatch`() {
        assertFailsWith<IllegalArgumentException> {
            AndroidMediaCopyRequests.normalizeSources(
                listOf(
                    AndroidMediaCopySource(
                        contentUri = "content://media/external/images/media/10",
                        displayName = "clip.mp4",
                        outputDisplayName = "clip (copy).mp4",
                        mimeType = "video/mp4",
                    ),
                ),
            )
        }
    }

    @Test
    fun `copy rejects unsafe output display names`() {
        assertFailsWith<IllegalArgumentException> {
            AndroidMediaCopyRequests.normalizeSources(
                listOf(
                    AndroidMediaCopySource(
                        contentUri = "content://media/external/images/media/10",
                        displayName = "photo.jpg",
                        outputDisplayName = "Trips/photo.jpg",
                        mimeType = "image/jpeg",
                    ),
                ),
            )
        }
    }
}
