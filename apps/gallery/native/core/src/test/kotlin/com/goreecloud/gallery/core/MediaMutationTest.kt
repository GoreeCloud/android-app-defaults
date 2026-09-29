package com.goreecloud.gallery.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaMutationTest {
    @Test
    fun deleteRequiresConfirmationWhenWriteGrantExists() {
        val mutation = MediaMutation(MutationKind.DELETE, setOf("media-1"))
        assertEquals(MutationDecision.Allowed(requiresConfirmation = true), MediaMutationPolicy.evaluate(mutation, true))
    }

    @Test
    fun mutationIsDeniedWithoutPlatformWriteGrant() {
        val mutation = MediaMutation(MutationKind.MOVE, setOf("media-1"), destinationAlbumId = "album-2")
        assertEquals(
            MutationDecision.Denied("platform-authorized media write access is required"),
            MediaMutationPolicy.evaluate(mutation, false),
        )
    }

    @Test
    fun moveRequiresDestination() {
        assertFailsWith<IllegalArgumentException> {
            MediaMutation(MutationKind.MOVE, setOf("media-1"))
        }
    }
}
