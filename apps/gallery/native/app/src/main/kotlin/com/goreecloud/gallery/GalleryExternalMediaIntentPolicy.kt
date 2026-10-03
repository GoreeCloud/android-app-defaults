package com.goreecloud.gallery

/**
 * Fail-closed contract for media URIs handed to Gallery by another Android component.
 *
 * The external viewer is intentionally narrower than Gallery's MediaStore library surface:
 * it accepts only explicit image/video content or file URIs for Android's VIEW and REVIEW
 * actions. It never treats http(s) or another remote scheme as a Gallery media source.
 */
internal object GalleryExternalMediaIntentPolicy {
    private const val ACTION_VIEW = "android.intent.action.VIEW"
    private const val ACTION_REVIEW = "android.provider.action.REVIEW"
    private const val ACTION_REVIEW_SECURE = "android.provider.action.REVIEW_SECURE"

    private val supportedActions = setOf(
        ACTION_VIEW,
        ACTION_REVIEW,
        ACTION_REVIEW_SECURE,
    )

    enum class Kind {
        IMAGE,
        VIDEO,
    }

    fun accepts(action: String?, scheme: String?, mimeType: String?): Boolean =
        action in supportedActions &&
            scheme?.lowercase() in setOf("content", "file") &&
            kindFor(mimeType) != null

    fun kindFor(mimeType: String?): Kind? {
        val normalized = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()

        return when {
            normalized.startsWith("image/") -> Kind.IMAGE
            normalized.startsWith("video/") -> Kind.VIDEO
            else -> null
        }
    }

    fun isSecureReview(action: String?): Boolean = action == ACTION_REVIEW_SECURE
}
