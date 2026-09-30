package com.goreecloud.gallery.android

import com.goreecloud.gallery.core.MediaStoreProjection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidMediaStoreProjectionTest {
    @Test
    fun `android framework columns match native core projection`() {
        assertEquals(MediaStoreProjection.columns, AndroidMediaStoreProjection.columns)
        assertEquals("datetaken", AndroidMediaStoreProjection.columns[3])
    }

    @Test
    fun `image rows project onto image media collection`() {
        assertEquals(
            "content://media/external/images/media",
            AndroidMediaStoreItemUris.collectionUriForMimeType(
                volumeName = "external",
                mimeType = "image/jpeg",
            ),
        )
    }

    @Test
    fun `video rows project onto video media collection`() {
        assertEquals(
            "content://media/external/video/media",
            AndroidMediaStoreItemUris.collectionUriForMimeType(
                volumeName = "external",
                mimeType = "VIDEO/MP4",
            ),
        )
    }

    @Test
    fun `non media rows fail closed`() {
        assertFailsWith<IllegalArgumentException> {
            AndroidMediaStoreItemUris.collectionUriForMimeType(
                volumeName = "external",
                mimeType = "application/pdf",
            )
        }
    }
}
