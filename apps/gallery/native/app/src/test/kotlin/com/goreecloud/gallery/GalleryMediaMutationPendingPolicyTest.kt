package com.goreecloud.gallery

import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import com.goreecloud.gallery.android.AndroidMediaMutationRequests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GalleryMediaMutationPendingPolicyTest {
    @Test
    fun `trash scope survives an exact save and recreation round trip`() {
        val first = "content://media/external/images/media/42"
        val second = "content://media/external/video/media/7"
        val captured = AndroidMediaMutationPendingStates.capture(
            AndroidMediaMutationMode.TRASH,
            listOf(first, second),
        )

        val restored = GalleryMediaMutationPendingPolicy.restore(
            AndroidMediaMutationPendingStates.modeName(captured),
            AndroidMediaMutationPendingStates.contentUriValues(captured).asList(),
        )

        assertEquals(AndroidMediaMutationMode.TRASH, restored?.mode)
        assertEquals(listOf(first, second), restored?.contentUris)
    }

    @Test
    fun `permanent delete scope survives an exact save and recreation round trip`() {
        val uri = "content://media/external_primary/images/media/99"
        val captured = AndroidMediaMutationPendingStates.capture(
            AndroidMediaMutationMode.DELETE,
            listOf(uri),
        )

        val restored = GalleryMediaMutationPendingPolicy.restore(
            AndroidMediaMutationPendingStates.modeName(captured),
            AndroidMediaMutationPendingStates.contentUriValues(captured).asList(),
        )

        assertEquals(AndroidMediaMutationMode.DELETE, restored?.mode)
        assertEquals(listOf(uri), restored?.contentUris)
    }

    @Test
    fun `main Gallery recreation cannot manufacture Recycle Bin restore authority`() {
        val uri = "content://media/external/images/media/42"

        assertNull(GalleryMediaMutationPendingPolicy.restore("RESTORE", listOf(uri)))
    }

    @Test
    fun `tampered saved mutation scope fails closed`() {
        val uri = "content://media/external/images/media/42"

        assertNull(GalleryMediaMutationPendingPolicy.restore("TRASH", listOf(" $uri")))
        assertNull(GalleryMediaMutationPendingPolicy.restore("DELETE", listOf(uri, uri)))
        assertNull(GalleryMediaMutationPendingPolicy.restore("DELETE", emptyList()))
    }

    @Test
    fun `pending mutation item count follows Android mutation bound`() {
        assertEquals(
            AndroidMediaMutationRequests.MAX_MUTATION_ITEMS,
            GalleryMediaMutationPendingPolicy.MAX_PENDING_CONTENT_URIS,
        )
        assertFalse(GalleryMediaMutationPendingPolicy.acceptsItemCount(0))
        assertTrue(GalleryMediaMutationPendingPolicy.acceptsItemCount(1))
        assertTrue(
            GalleryMediaMutationPendingPolicy.acceptsItemCount(
                GalleryMediaMutationPendingPolicy.MAX_PENDING_CONTENT_URIS,
            ),
        )
        assertFalse(
            GalleryMediaMutationPendingPolicy.acceptsItemCount(
                GalleryMediaMutationPendingPolicy.MAX_PENDING_CONTENT_URIS + 1,
            ),
        )
    }

    @Test
    fun `oversized saved mutation scope fails closed`() {
        val oversized = (1..GalleryMediaMutationPendingPolicy.MAX_PENDING_CONTENT_URIS + 1).map { id ->
            "content://media/external/images/media/$id"
        }

        assertNull(GalleryMediaMutationPendingPolicy.restore("TRASH", oversized))
    }
}
