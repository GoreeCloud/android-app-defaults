package com.goreecloud.gallery.core

data class MediaAlbumOption(
    val albumId: String,
    val albumName: String,
    val itemCount: Int,
) {
    init {
        require(albumId.isNotBlank())
        require(albumName.isNotBlank())
        require(itemCount > 0)
    }
}

fun mediaAlbumOptions(items: List<MediaItem>): List<MediaAlbumOption> {
    data class MutableAlbum(val id: String, val name: String, var count: Int)

    val albums = linkedMapOf<String, MutableAlbum>()
    items.forEach { item ->
        val albumId = item.albumId ?: return@forEach
        val albumName = item.albumName ?: return@forEach
        val current = albums[albumId]
        if (current == null) {
            albums[albumId] = MutableAlbum(albumId, albumName, 1)
        } else {
            current.count += 1
        }
    }
    return albums.values.map { MediaAlbumOption(it.id, it.name, it.count) }
}

fun filterAuthorizedAlbum(items: List<MediaItem>, albumId: String?): List<MediaItem> {
    val normalized = albumId?.trim().orEmpty()
    if (normalized.isEmpty()) return items
    return items.filter { it.albumId == normalized }
}
