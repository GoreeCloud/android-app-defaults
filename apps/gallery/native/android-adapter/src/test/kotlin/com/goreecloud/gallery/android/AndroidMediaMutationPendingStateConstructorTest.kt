package com.goreecloud.gallery.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AndroidMediaMutationPendingStateConstructorTest {
    @Test
    fun `pending state constructor defensively seals uri scope`() {
        val first = "content://media/external/images/media/42"
        val second = "content://media/external/video/media/7"
        val supplied = mutableListOf(first)

        val state = AndroidMediaMutationPendingState(
            mode = AndroidMediaMutationMode.TRASH,
            contentUris = supplied,
        )

        supplied += second
        assertEquals(listOf(first), state.contentUris)
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (state.contentUris as MutableList<String>).add(second)
        }
        assertEquals(listOf(first), state.contentUris)
    }
}
