package com.goreecloud.gallery

import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryMediaMutationSavedStateTest {
    @Test
    fun `capture and restore round trip exact pending state`() {
        val uri = "content://media/external/images/media/42"
        val pending = AndroidMediaMutationPendingStates.capture(
            AndroidMediaMutationMode.TRASH,
            listOf(uri),
        )

        val saved = GalleryMediaMutationSavedStates.capture(pending)
        val restored = GalleryMediaMutationSavedStates.restore(
            saved.schemaVersion,
            saved.modeName,
            saved.contentUriValues().asList(),
            saved.fingerprint,
        )

        assertEquals(GalleryMediaMutationSavedStates.SCHEMA_VERSION, saved.schemaVersion)
        assertEquals(GalleryMediaMutationSavedStates.FINGERPRINT_HEX_LENGTH, saved.fingerprint.length)
        assertTrue(saved.fingerprint.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(AndroidMediaMutationMode.TRASH, restored?.mode)
        assertEquals(listOf(uri), restored?.contentUris)
    }

    @Test
    fun `saved content uri values are defensively copied`() {
        val uri = "content://media/external/images/media/42"
        val pending = AndroidMediaMutationPendingStates.capture(
            AndroidMediaMutationMode.DELETE,
            listOf(uri),
        )
        val saved = GalleryMediaMutationSavedStates.capture(pending)

        val first = saved.contentUriValues()
        first[0] = "content://media/external/images/media/99"

        assertEquals(arrayOf(uri).toList(), saved.contentUriValues().toList())
    }

    @Test
    fun `capture rejects recycle bin restore authority`() {
        val pending = AndroidMediaMutationPendingStates.capture(
            AndroidMediaMutationMode.RESTORE,
            listOf("content://media/external/images/media/42"),
        )

        assertFailsWith<IllegalArgumentException> {
            GalleryMediaMutationSavedStates.capture(pending)
        }
    }

    @Test
    fun `restore rejects missing or unsupported schema version`() {
        val uri = "content://media/external/images/media/42"
        val saved = GalleryMediaMutationSavedStates.capture(
            AndroidMediaMutationPendingStates.capture(AndroidMediaMutationMode.TRASH, listOf(uri)),
        )

        assertNull(GalleryMediaMutationSavedStates.restore(null, "TRASH", listOf(uri), saved.fingerprint))
        assertNull(GalleryMediaMutationSavedStates.restore(0, "TRASH", listOf(uri), saved.fingerprint))
        assertNull(
            GalleryMediaMutationSavedStates.restore(
                GalleryMediaMutationSavedStates.SCHEMA_VERSION + 1,
                "TRASH",
                listOf(uri),
                saved.fingerprint,
            ),
        )
    }

    @Test
    fun `restore cannot manufacture recycle bin restore authority`() {
        val uri = "content://media/external/images/media/42"
        val saved = GalleryMediaMutationSavedStates.capture(
            AndroidMediaMutationPendingStates.capture(AndroidMediaMutationMode.TRASH, listOf(uri)),
        )

        assertNull(
            GalleryMediaMutationSavedStates.restore(
                GalleryMediaMutationSavedStates.SCHEMA_VERSION,
                "RESTORE",
                listOf(uri),
                saved.fingerprint,
            ),
        )
    }

    @Test
    fun `restore fails closed on tampered uri scope mode and fingerprint`() {
        val uri = "content://media/external/images/media/42"
        val saved = GalleryMediaMutationSavedStates.capture(
            AndroidMediaMutationPendingStates.capture(AndroidMediaMutationMode.DELETE, listOf(uri)),
        )

        assertNull(
            GalleryMediaMutationSavedStates.restore(
                saved.schemaVersion,
                saved.modeName,
                listOf("content://media/external/images/media/99"),
                saved.fingerprint,
            ),
        )
        assertNull(
            GalleryMediaMutationSavedStates.restore(
                saved.schemaVersion,
                "TRASH",
                saved.contentUriValues().asList(),
                saved.fingerprint,
            ),
        )
        assertNull(
            GalleryMediaMutationSavedStates.restore(
                saved.schemaVersion,
                saved.modeName,
                saved.contentUriValues().asList(),
                "0".repeat(GalleryMediaMutationSavedStates.FINGERPRINT_HEX_LENGTH),
            ),
        )
        assertNull(
            GalleryMediaMutationSavedStates.restore(
                saved.schemaVersion,
                saved.modeName,
                saved.contentUriValues().asList(),
                null,
            ),
        )
    }
}
