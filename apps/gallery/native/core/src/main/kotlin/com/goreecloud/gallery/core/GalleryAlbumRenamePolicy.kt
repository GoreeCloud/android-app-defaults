package com.goreecloud.gallery.core

data class GalleryAlbumRenameSource(
    val albumId: String,
    val currentName: String,
    val volumeName: String,
    val sourceRelativePath: String,
    val parentRelativePath: String,
    val contentUris: List<String>,
)

data class GalleryAlbumRenameDestination(
    val source: GalleryAlbumRenameSource,
    val newName: String,
    val destinationRelativePath: String,
)

object GalleryAlbumRenamePolicy {
    fun sourceForAlbum(
        currentScope: List<MediaItem>,
        albumId: String,
    ): GalleryAlbumRenameSource? {
        val normalizedAlbumId = albumId.trim()
        if (normalizedAlbumId.isEmpty()) return null

        val items = currentScope.filter { it.albumId == normalizedAlbumId }
        if (items.isEmpty()) return null

        val names = items.mapNotNull { it.albumName?.trim()?.takeIf(String::isNotEmpty) }.distinct()
        val volumes = items.mapNotNull { it.volumeName?.trim()?.takeIf(String::isNotEmpty) }.distinct()
        val paths = items.mapNotNull { canonicalProviderPath(it.relativePath) }.distinct()
        if (
            names.size != 1 ||
            volumes.size != 1 ||
            paths.size != 1 ||
            items.any { it.albumName.isNullOrBlank() || it.volumeName.isNullOrBlank() || canonicalProviderPath(it.relativePath) == null }
        ) {
            return null
        }

        val sourcePath = paths.single()
        val segments = sourcePath.trimEnd('/').split('/').filter(String::isNotEmpty)
        if (segments.size < 2) return null

        val currentName = names.single()
        val leafName = segments.last()
        if (!currentName.equals(leafName, ignoreCase = true)) return null

        val contentUris = items.map { it.contentUri }.distinct()
        if (contentUris.size != items.size) return null

        return GalleryAlbumRenameSource(
            albumId = normalizedAlbumId,
            currentName = currentName,
            volumeName = volumes.single(),
            sourceRelativePath = sourcePath,
            parentRelativePath = segments.dropLast(1).joinToString(separator = "/", postfix = "/"),
            contentUris = contentUris,
        )
    }

    fun destinationForAlbum(
        currentScope: List<MediaItem>,
        albumId: String,
        rawName: String,
    ): GalleryAlbumRenameDestination {
        val source = requireNotNull(sourceForAlbum(currentScope, albumId)) {
            "This album cannot be safely renamed from the current media snapshot"
        }
        val newName = GalleryNewFolderMovePolicy.normalizeFolderName(rawName)
        require(!newName.equals(source.currentName, ignoreCase = true)) {
            "Choose a different album name"
        }

        val destinationPath = source.parentRelativePath + newName + "/"
        val collision = currentScope.any { item ->
            item.albumId != source.albumId &&
                item.volumeName?.trim() == source.volumeName &&
                canonicalProviderPath(item.relativePath)?.equals(destinationPath, ignoreCase = true) == true
        }
        require(!collision) { "An album with this name already exists here" }

        return GalleryAlbumRenameDestination(
            source = source,
            newName = newName,
            destinationRelativePath = destinationPath,
        )
    }

    private fun canonicalProviderPath(raw: String?): String? {
        val value = raw?.trim()?.replace('\\', '/') ?: return null
        if (value.isEmpty() || value.startsWith('/') || "://" in value || '\u0000' in value) return null
        val segments = value.split('/').filter(String::isNotEmpty)
        if (segments.isEmpty() || segments.any { it == "." || it == ".." }) return null
        return segments.joinToString(separator = "/", postfix = "/")
    }
}
