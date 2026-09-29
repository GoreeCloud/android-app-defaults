package com.goreecloud.gallery.core

enum class MediaTypeFilter {
    ALL,
    IMAGES,
    VIDEOS,
}

fun MediaTypeFilter.filter(items: List<MediaItem>): List<MediaItem> = when (this) {
    MediaTypeFilter.ALL -> items
    MediaTypeFilter.IMAGES -> items.filter { it.kind == MediaKind.IMAGE }
    MediaTypeFilter.VIDEOS -> items.filter { it.kind == MediaKind.VIDEO }
}
