package com.goreecloud.gallery.core

data class GalleryNewFolderCopyDestination(
    val displayName: String,
    val parentDisplayName: String,
    val relativePath: String,
) {
    init {
        require(displayName.isNotBlank())
        require(parentDisplayName.isNotBlank())
        require(relativePath.isNotBlank())
    }
}

object GalleryNewFolderCopyPolicy {
    fun parentForSelection(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
    ): String? {
        val selected = resolveSelection(currentScope, selectedContentUris) ?: return null
        val hasImages = selected.any { it.mimeType.startsWith("image/") }
        val hasVideos = selected.any { it.mimeType.startsWith("video/") }
        if (!hasImages && !hasVideos) return null

        return when {
            hasImages && hasVideos -> "DCIM"
            hasVideos -> "Movies"
            else -> "Pictures"
        }
    }

    fun destinationForSelection(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
        rawFolderName: String,
    ): GalleryNewFolderCopyDestination {
        val parent = parentForSelection(currentScope, selectedContentUris)
            ?: throw IllegalArgumentException("selection cannot establish copy destination authority")
        val name = GalleryNewFolderMovePolicy.normalizeFolderName(rawFolderName)
        val relativePath = "$parent/$name/"

        require(
            currentScope.none { item ->
                item.relativePath?.trimEnd('/')?.equals(relativePath.trimEnd('/'), ignoreCase = true) == true
            },
        ) { "a visible authorized destination already uses this folder path" }

        return GalleryNewFolderCopyDestination(
            displayName = name,
            parentDisplayName = parent,
            relativePath = relativePath,
        )
    }

    private fun resolveSelection(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
    ): List<MediaItem>? {
        if (selectedContentUris.isEmpty()) return null
        val byUri = currentScope.associateBy { it.contentUri }
        return selectedContentUris.map { uri -> byUri[uri] ?: return null }
    }
}
