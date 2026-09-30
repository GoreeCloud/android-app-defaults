package com.goreecloud.gallery.android

import java.util.Collections

/**
 * Minimal state that may be retained while Android owns a MediaStore mutation confirmation flow.
 *
 * This is deliberately not a data class: a generated copy() surface would let callers manufacture
 * altered pending state without going back through the canonical capture/restore validation
 * boundary. The URI list is also exposed as an unmodifiable defensive copy.
 *
 * This is not a second Trash database and does not grant mutation authority. It only preserves the
 * exact already-requested mutation mode and bounded canonical MediaStore item URIs so an Activity
 * can reconcile Android's later result after ordinary recreation. The actual mutation remains
 * owned and confirmed by Android MediaStore.
 */
class AndroidMediaMutationPendingState internal constructor(
    val mode: AndroidMediaMutationMode,
    contentUris: List<String>,
) {
    val contentUris: List<String> = Collections.unmodifiableList(ArrayList(contentUris))
}

object AndroidMediaMutationPendingStates {
    fun capture(
        mode: AndroidMediaMutationMode,
        contentUris: Collection<String>,
    ): AndroidMediaMutationPendingState = AndroidMediaMutationPendingState(
        mode = mode,
        contentUris = AndroidMediaMutationRequests.normalizeMediaStoreUris(contentUris),
    )

    /**
     * Restore only exact enum state and the exact canonical URI list previously emitted by capture.
     *
     * Missing, unknown, malformed, broad, oversized, duplicated, reordered-by-normalization, or
     * whitespace-altered saved state is discarded rather than normalized into new pending authority.
     */
    fun restore(
        modeName: String?,
        contentUris: Collection<String>?,
    ): AndroidMediaMutationPendingState? {
        if (modeName.isNullOrBlank() || contentUris.isNullOrEmpty()) return null
        val mode = AndroidMediaMutationMode.entries.firstOrNull { it.name == modeName } ?: return null
        val supplied = contentUris.toList()
        val canonical = try {
            AndroidMediaMutationRequests.normalizeMediaStoreUris(supplied)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (supplied != canonical) return null
        return AndroidMediaMutationPendingState(mode = mode, contentUris = canonical)
    }

    fun modeName(state: AndroidMediaMutationPendingState): String = state.mode.name

    fun contentUriValues(state: AndroidMediaMutationPendingState): Array<String> =
        state.contentUris.toTypedArray()
}
