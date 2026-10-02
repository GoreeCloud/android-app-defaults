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
        val sourcePaths = selected.mapNotNull { it.relativePath?.trim()?.takeIf(String::isNotEmpty) }.toSet()

        return currentScope
            .asSequence()
            .filter { item ->
                !item.albumId.isNullOrBlank() &&
                    !item.albumName.isNullOrBlank() &&
                    !item.relativePath.isNullOrBlank() &&
                    item.relativePath !in sourcePaths
            }
            .groupBy { checkNotNull(it.albumId) }
            .mapNotNull { (albumId, items) ->
                val names = items.mapNotNull { it.albumName }.distinct()
                val paths = items.mapNotNull { it.relativePath }.distinct()
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
}
