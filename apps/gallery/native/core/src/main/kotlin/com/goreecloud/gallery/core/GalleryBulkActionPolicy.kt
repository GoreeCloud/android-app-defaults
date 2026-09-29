package com.goreecloud.gallery.core

/**
 * Framework-independent planning for non-destructive multi-selection actions.
 *
 * Callers must supply the current authorized/presented scope. The policy resolves selections through
 * [GallerySelectionPolicy], so stale or foreign content URIs cannot enter a bulk action plan.
 */
object GalleryBulkActionPolicy {
    fun sharePlan(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
    ): GallerySharePlan? {
        val items = GallerySelectionPolicy.resolve(currentScope, selectedContentUris)
        if (items.isEmpty()) return null

        val mimeType = when {
            items.map { it.mimeType }.distinct().size == 1 -> items.first().mimeType
            items.all { it.mimeType.startsWith("image/") } -> "image/*"
            items.all { it.mimeType.startsWith("video/") } -> "video/*"
            else -> "*/*"
        }
        return GallerySharePlan(
            mimeType = mimeType,
            contentUris = items.map { it.contentUri },
        )
    }

    fun favoriteAction(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
        favoriteContentUris: Set<String>,
    ): GalleryFavoriteBulkAction? {
        val items = GallerySelectionPolicy.resolve(currentScope, selectedContentUris)
        if (items.isEmpty()) return null
        return if (items.all { it.contentUri in favoriteContentUris }) {
            GalleryFavoriteBulkAction.REMOVE
        } else {
            GalleryFavoriteBulkAction.ADD
        }
    }
}

data class GallerySharePlan(
    val mimeType: String,
    val contentUris: List<String>,
)

enum class GalleryFavoriteBulkAction {
    ADD,
    REMOVE,
}
