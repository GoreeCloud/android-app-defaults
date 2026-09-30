package com.goreecloud.gallery.android

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AndroidMediaMutationCanonicalPathTest {
    @Test
    fun `mutation scope rejects path aliases for the same MediaStore item`() {
        listOf(
            "content://media/external//images/media/42",
            "content://media/external/images/media/42/",
            "content://media//external/images/media/42",
            "content://media/external/images/media/042",
            "content://media/external/images/media/+42",
        ).forEach { uri ->
            assertFailsWith<IllegalArgumentException>(uri) {
                AndroidMediaMutationRequests.normalizeMediaStoreUris(listOf(uri))
            }
        }
    }
}
