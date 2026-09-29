package com.goreecloud.gallery

import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryMediaMutationBundleContractTest {
    @Test
    fun `capture and restore preserve exact validated mutation scope`() {
        val uris = listOf(
            "content://media/external/images/media/42",
            "content://media/external/video/media/43",
        )
        val pending = AndroidMediaMutationPendingStates.capture(AndroidMediaMutationMode.DELETE, uris)

        val fields = GalleryMediaMutationBundleContract.capture(pending)
        val restored = GalleryMediaMutationBundleContract.restore(fields)

        assertEquals(GalleryMediaMutationSavedStates.SCHEMA_VERSION, fields.schemaVersion)
        assertEquals("DELETE", fields.modeName)
        assertEquals(uris, fields.contentUriValues().toList())
        assertEquals(AndroidMediaMutationMode.DELETE, restored?.mode)
        assertEquals(uris, restored?.contentUris)
    }

    @Test
    fun `bundle fields defensively copy uri scope`() {
        val pending = AndroidMediaMutationPendingStates.capture(
            AndroidMediaMutationMode.TRASH,
            listOf("content://media/external/images/media/42"),
        )
        val fields = GalleryMediaMutationBundleContract.capture(pending)

        val first = fields.contentUriValues()
        first[0] = "content://media/external/images/media/99"

        assertEquals(
            listOf("content://media/external/images/media/42"),
            fields.contentUriValues().toList(),
        )
    }

    @Test
    fun `bundle contract uses stable distinct keys`() {
        val keys = setOf(
            GalleryMediaMutationBundleContract.SCHEMA_VERSION_KEY,
            GalleryMediaMutationBundleContract.MODE_KEY,
            GalleryMediaMutationBundleContract.CONTENT_URIS_KEY,
            GalleryMediaMutationBundleContract.FINGERPRINT_KEY,
        )
        assertEquals(4, keys.size)
        assertEquals("pending_media_mutation_mode", GalleryMediaMutationBundleContract.MODE_KEY)
        assertEquals("pending_media_mutation_uris", GalleryMediaMutationBundleContract.CONTENT_URIS_KEY)
    }

    @Test
    fun `restore of absent bundle fields returns no pending authority`() {
        assertNull(GalleryMediaMutationBundleContract.restore(null))
    }
}
