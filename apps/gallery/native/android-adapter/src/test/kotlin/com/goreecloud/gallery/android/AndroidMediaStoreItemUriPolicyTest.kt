package com.goreecloud.gallery.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AndroidMediaStoreItemUriPolicyTest {
    @Test
    fun `accepts canonical image and video item uris`() {
        val image = "content://media/external/images/media/42"
        val video = "content://media/external_primary/video/media/7"

        assertEquals(image, AndroidMediaStoreItemUriPolicy.requireCanonicalItemUri(image))
        assertEquals(video, AndroidMediaStoreItemUriPolicy.requireCanonicalItemUri(video))
        assertTrue(AndroidMediaStoreItemUriPolicy.isCanonicalItemUri(image))
        assertTrue(AndroidMediaStoreItemUriPolicy.isCanonicalItemUri(video))
    }

    @Test
    fun `rejects collection arbitrary provider and non canonical authority forms`() {
        val rejected = listOf(
            "content://media/external/images/media",
            "content://media/external/file/42",
            "content://example/external/images/media/42",
            "content://user@media/external/images/media/42",
            "content://media:12/external/images/media/42",
        )
        rejected.forEach { uri ->
            assertFalse(AndroidMediaStoreItemUriPolicy.isCanonicalItemUri(uri), uri)
            assertFailsWith<IllegalArgumentException> {
                AndroidMediaStoreItemUriPolicy.requireCanonicalItemUri(uri)
            }
        }
    }

    @Test
    fun `rejects whitespace query fragment encoded controls and invalid ids`() {
        val rejected = listOf(
            " content://media/external/images/media/42",
            "content://media/external/images/media/42 ",
            "content://media/external/images/media/42?x=1",
            "content://media/external/images/media/42#fragment",
            "content://media/external/images/media/%34%32",
            "content://media/external/images/media/0",
            "content://media/external/images/media/01",
            "content://media/external/images/media/not-a-number",
        )
        rejected.forEach { uri ->
            assertFalse(AndroidMediaStoreItemUriPolicy.isCanonicalItemUri(uri), uri)
        }
    }

    @Test
    fun `rejects overlong uri`() {
        val volume = "v".repeat(AndroidMediaStoreItemUriPolicy.MAX_CONTENT_URI_CHARACTERS)
        val uri = "content://media/$volume/images/media/1"
        assertTrue(uri.length > AndroidMediaStoreItemUriPolicy.MAX_CONTENT_URI_CHARACTERS)
        assertFalse(AndroidMediaStoreItemUriPolicy.isCanonicalItemUri(uri))
    }
}
