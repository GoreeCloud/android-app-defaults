package com.goreecloud.gallery

import com.goreecloud.gallery.android.AndroidMediaMutationPendingState

/**
 * Pure field contract for persisting a pending MediaStore mutation into an
 * Activity Bundle. Android Bundle reads/writes remain in GalleryActivity; this
 * value defines the exact versioned fields and stable keys they must use.
 */
internal class GalleryMediaMutationBundleFields internal constructor(
    val schemaVersion: Int,
    val modeName: String,
    contentUris: Array<String>,
    val fingerprint: String,
) {
    private val frozenContentUris = contentUris.copyOf()

    fun contentUriValues(): Array<String> = frozenContentUris.copyOf()
}

internal object GalleryMediaMutationBundleContract {
    const val SCHEMA_VERSION_KEY = "pending_media_mutation_schema_version"
    const val MODE_KEY = "pending_media_mutation_mode"
    const val CONTENT_URIS_KEY = "pending_media_mutation_uris"
    const val FINGERPRINT_KEY = "pending_media_mutation_fingerprint"

    fun capture(state: AndroidMediaMutationPendingState): GalleryMediaMutationBundleFields {
        val saved = GalleryMediaMutationSavedStates.capture(state)
        return GalleryMediaMutationBundleFields(
            schemaVersion = saved.schemaVersion,
            modeName = saved.modeName,
            contentUris = saved.contentUriValues(),
            fingerprint = saved.fingerprint,
        )
    }

    fun restore(fields: GalleryMediaMutationBundleFields?): AndroidMediaMutationPendingState? {
        if (fields == null) return null
        return GalleryMediaMutationSavedStates.restore(
            schemaVersion = fields.schemaVersion,
            modeName = fields.modeName,
            contentUris = fields.contentUriValues().asList(),
            fingerprint = fields.fingerprint,
        )
    }
}
