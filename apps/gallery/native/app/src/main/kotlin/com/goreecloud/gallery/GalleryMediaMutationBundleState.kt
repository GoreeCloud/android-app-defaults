package com.goreecloud.gallery

import android.os.Bundle
import com.goreecloud.gallery.android.AndroidMediaMutationPendingState

/**
 * Android Bundle adapter for the versioned pending MediaStore mutation field
 * contract. It persists only the exact fields covered by the lifecycle
 * fingerprint and restores authority only through the validated core contract.
 */
internal object GalleryMediaMutationBundleState {
    fun save(outState: Bundle, state: AndroidMediaMutationPendingState) {
        val fields = GalleryMediaMutationBundleContract.capture(state)
        outState.putInt(
            GalleryMediaMutationBundleContract.SCHEMA_VERSION_KEY,
            fields.schemaVersion,
        )
        outState.putString(
            GalleryMediaMutationBundleContract.MODE_KEY,
            fields.modeName,
        )
        outState.putStringArray(
            GalleryMediaMutationBundleContract.CONTENT_URIS_KEY,
            fields.contentUriValues(),
        )
        outState.putString(
            GalleryMediaMutationBundleContract.FINGERPRINT_KEY,
            fields.fingerprint,
        )
    }

    fun restore(savedInstanceState: Bundle?): AndroidMediaMutationPendingState? {
        if (savedInstanceState == null ||
            !savedInstanceState.containsKey(GalleryMediaMutationBundleContract.SCHEMA_VERSION_KEY)
        ) return null

        val modeName = savedInstanceState.getString(GalleryMediaMutationBundleContract.MODE_KEY) ?: return null
        val contentUris = savedInstanceState.getStringArray(GalleryMediaMutationBundleContract.CONTENT_URIS_KEY) ?: return null
        val fingerprint = savedInstanceState.getString(GalleryMediaMutationBundleContract.FINGERPRINT_KEY) ?: return null

        return GalleryMediaMutationBundleContract.restore(
            GalleryMediaMutationBundleFields(
                schemaVersion = savedInstanceState.getInt(GalleryMediaMutationBundleContract.SCHEMA_VERSION_KEY),
                modeName = modeName,
                contentUris = contentUris,
                fingerprint = fingerprint,
            ),
        )
    }
}
