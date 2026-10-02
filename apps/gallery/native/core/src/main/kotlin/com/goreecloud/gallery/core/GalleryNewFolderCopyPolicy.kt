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
        val context = resolveSelection(currentScope, selectedContentUris) ?: return null
        val hasImages = context.items.any { it.mimeType.startsWith("image/") }
        val hasVideos = context.items.any { it.mimeType.startsWith("video/") }
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
        val context = resolveSelection(currentScope, selectedContentUris)
            ?: throw IllegalArgumentException("selection cannot establish single-volume copy destination authority")
        val hasImages = context.items.any { it.mimeType.startsWith("image/") }
        val hasVideos = context.items.any { it.mimeType.startsWith("video/") }
        val parent = when {
            hasImages && hasVideos -> "DCIM"
            hasVideos -> "Movies"
            hasImages -> "Pictures"
            else -> throw IllegalArgumentException("selection does not contain copyable media")
        }
        val name = GalleryNewFolderMovePolicy.normalizeFolderName(rawFolderName)
        val relativePath = "$parent/$name/"

        require(
            currentScope.none { item ->
                normalizeVolumeName(item.volumeName) == context.volumeName &&
                    canonicalProviderPath(item.relativePath)?.equals(relativePath, ignoreCase = true) == true
            },
        ) { "a visible authorized destination already uses this folder path on the selected volume" }

        return GalleryNewFolderCopyDestination(
            displayName = name,
            parentDisplayName = parent,
            relativePath = relativePath,
        )
    }

    private fun resolveSelection(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
    ): CopySelectionContext? {
        if (selectedContentUris.isEmpty()) return null
        val byUri = currentScope.associateBy { it.contentUri }
        val items = selectedContentUris.map { uri -> byUri[uri] ?: return null }
        val volumes = items.mapNotNull { normalizeVolumeName(it.volumeName) }.distinct()
        if (volumes.size != 1 || items.any { normalizeVolumeName(it.volumeName) != volumes.single() }) return null
        return CopySelectionContext(items = items, volumeName = volumes.single())
    }

    private fun normalizeVolumeName(raw: String?): String? =
        raw?.trim()?.takeIf(String::isNotEmpty)

    private fun canonicalProviderPath(raw: String?): String? {
        val value = raw?.trim()?.replace('\\', '/') ?: return null
        if (value.isEmpty() || value.startsWith('/') || "://" in value || '\u0000' in value) return null
        val segments = value.split('/').filter(String::isNotEmpty)
        if (segments.isEmpty() || segments.any { it == "." || it == ".." }) return null
        return segments.joinToString(separator = "/", postfix = "/")
    }

    private data class CopySelectionContext(
        val items: List<MediaItem>,
        val volumeName: String,
    )
}
