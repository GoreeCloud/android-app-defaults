package com.goreecloud.gallery

import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingState
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import com.goreecloud.gallery.android.AndroidMediaMutationRequests

/**
 * GalleryActivity-specific boundary for restoring an Android-owned MediaStore mutation request.
 *
 * The main Gallery surface may originate only ordinary Trash or permanent Delete requests. Saved
 * Activity state therefore must never manufacture Recycle Bin Restore authority after recreation.
 * Restoration uses the same item bound as the Android mutation adapter so lifecycle state cannot
 * claim a broader mutation scope than the Android confirmation request that originally created it.
 */
internal object GalleryMediaMutationPendingPolicy {
    internal val MAX_PENDING_CONTENT_URIS: Int = AndroidMediaMutationRequests.MAX_MUTATION_ITEMS

    fun acceptsItemCount(itemCount: Int): Boolean =
        itemCount in 1..MAX_PENDING_CONTENT_URIS

    fun restore(
        modeName: String?,
        contentUris: Collection<String>?,
    ): AndroidMediaMutationPendingState? {
        if (contentUris == null || !acceptsItemCount(contentUris.size)) return null

        return AndroidMediaMutationPendingStates.restore(modeName, contentUris)?.takeIf { state ->
            state.mode == AndroidMediaMutationMode.TRASH || state.mode == AndroidMediaMutationMode.DELETE
        }
    }
}
