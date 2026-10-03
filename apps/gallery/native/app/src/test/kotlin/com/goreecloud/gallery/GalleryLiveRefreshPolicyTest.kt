package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryLiveRefreshPolicyTest {
    @Test
    fun `foreground library refresh proceeds when no conflicting surface or mutation is active`() {
        assertFalse(
            GalleryLiveRefreshPolicy.shouldDefer(
                viewerOpen = false,
                mediaMutationPending = false,
                mediaMovePending = false,
                mediaMoveExecutionInProgress = false,
                mediaCopyExecutionInProgress = false,
            ),
        )
    }

    @Test
    fun `foreground library refresh defers around viewer and media mutations`() {
        val cases = listOf(
            booleanArrayOf(true, false, false, false, false),
            booleanArrayOf(false, true, false, false, false),
            booleanArrayOf(false, false, true, false, false),
            booleanArrayOf(false, false, false, true, false),
            booleanArrayOf(false, false, false, false, true),
        )

        cases.forEach { state ->
            assertTrue(
                GalleryLiveRefreshPolicy.shouldDefer(
                    viewerOpen = state[0],
                    mediaMutationPending = state[1],
                    mediaMovePending = state[2],
                    mediaMoveExecutionInProgress = state[3],
                    mediaCopyExecutionInProgress = state[4],
                ),
            )
        }
    }
}
