package com.goreecloud.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GalleryExternalMediaIntentPolicyTest {
    @Test
    fun acceptsLocalImageAndVideoViewRequests() {
        assertTrue(GalleryExternalMediaIntentPolicy.accepts("android.intent.action.VIEW", "content", "image/jpeg"))
        assertTrue(GalleryExternalMediaIntentPolicy.accepts("android.provider.action.REVIEW", "file", "video/mp4"))
    }

    @Test
    fun rejectsRemoteAndUnsupportedRequests() {
        assertFalse(GalleryExternalMediaIntentPolicy.accepts("android.intent.action.VIEW", "https", "image/jpeg"))
        assertFalse(GalleryExternalMediaIntentPolicy.accepts("android.intent.action.VIEW", "content", "application/pdf"))
    }

    @Test
    fun normalizesMimeTypeAndSecureReviewState() {
        assertEquals(
            GalleryExternalMediaIntentPolicy.Kind.IMAGE,
            GalleryExternalMediaIntentPolicy.kindFor(" IMAGE/PNG ; charset=binary "),
        )
        assertEquals(
            GalleryExternalMediaIntentPolicy.Kind.VIDEO,
            GalleryExternalMediaIntentPolicy.kindFor("video/webm"),
        )
        assertTrue(GalleryExternalMediaIntentPolicy.isSecureReview("android.provider.action.REVIEW_SECURE"))
        assertFalse(GalleryExternalMediaIntentPolicy.isSecureReview("android.intent.action.VIEW"))
    }
}
