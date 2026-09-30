package com.goreecloud.gallery.core

enum class MediaSortOrder {
    NEWEST,
    OLDEST,
}

fun MediaSortOrder.sort(items: List<MediaItem>): List<MediaItem> {
    val indexed = items.withIndex()
    val comparator = Comparator<IndexedValue<MediaItem>> { left, right ->
        val leftTime = left.value.capturedAt ?: left.value.modifiedAt
        val rightTime = right.value.capturedAt ?: right.value.modifiedAt
        val timeOrder = when (this) {
            MediaSortOrder.NEWEST -> rightTime.compareTo(leftTime)
            MediaSortOrder.OLDEST -> leftTime.compareTo(rightTime)
        }
        if (timeOrder != 0) timeOrder else left.index.compareTo(right.index)
    }
    return indexed.sortedWith(comparator).map { it.value }
}
