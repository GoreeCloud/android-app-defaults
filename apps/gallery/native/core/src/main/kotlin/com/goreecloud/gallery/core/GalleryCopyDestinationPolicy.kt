package com.goreecloud.gallery.core

data class GalleryCopyDestination(
    val albumId: String,
    val displayName: String,
    val relativePath: String,
    val itemCount: Int,
) {
    init {
        require(albumId.isNotBlank())
        require(displayName.isNotBlank())
        require(relativePath.isNotBlank())
        require(itemCount > 0)
    }
}

object GalleryCopyDestinationPolicy {
    fun existingDestinations(
        currentScope: List<MediaItem>,
        selectedContentUris: Set<String>,
    ): List<GalleryCopyDestination> {
        if (selectedContentUris.isEmpty()) return emptyList()

        val byUri = currentScope.associateBy { it.contentUri }
        val selected = selectedContentUris.map { uri -> byUri[uri] ?: return emptyList() }
        val selectedVolume = selected
            .mapNotNull { normalizeVolumeName(it.volumeName) }
            .distinct()
            .singleOrNull()
            ?: return emptyList()
        if (selected.any { normalizeVolumeName(it.volumeName) != selectedVolume }) return emptyList()

        val selectedPaths = selected.map { canonicalProviderPath(it.relativePath) }
        if (selectedPaths.any { it == null }) return emptyList()
        val sourcePaths = selectedPaths.filterNotNull().toSet()

        return currentScope
            .asSequence()
            .filter { item ->
                normalizeVolumeName(item.volumeName) == selectedVolume &&
                    !item.albumId.isNullOrBlank() &&
                    !item.albumName.isNullOrBlank() &&
                    canonicalProviderPath(item.relativePath) != null &&
                    canonicalProviderPath(item.relativePath) !in sourcePaths
            }
            .groupBy { checkNotNull(it.albumId) }
            .mapNotNull { (albumId, items) ->
                val names = items.mapNotNull { it.albumName?.trim()?.takeIf(String::isNotEmpty) }.distinct()
                val paths = items.mapNotNull { canonicalProviderPath(it.relativePath) }.distinct()
                if (names.size != 1 || paths.size != 1) return@mapNotNull null
                GalleryCopyDestination(
                    albumId = albumId,
                    displayName = names.single(),
                    relativePath = paths.single(),
                    itemCount = items.size,
                )
            }
            .distinctBy { it.relativePath.lowercase() }
            .sortedWith(compareBy<GalleryCopyDestination> { it.displayName.lowercase() }.thenBy { it.albumId })
            .toList()
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
}
