package com.goreecloud.gallery

import com.goreecloud.gallery.core.MediaItem
import com.goreecloud.gallery.core.MediaSortOrder

enum class GalleryFileLoadingPriority(
    val storedValue: String,
    val label: String,
    val thumbnailWorkerCount: Int,
) {
    SLOW("slow", "Slow", 1),
    FAST("fast", "Fast", 4),
    ;

    companion object {
        fun fromStored(value: String?): GalleryFileLoadingPriority =
            entries.firstOrNull { it.storedValue == value } ?: FAST
    }
}

enum class GalleryGroupingMode(
    val storedValue: String,
    val label: String,
) {
    DAY("day", "Day"),
    MONTH("month", "Month"),
    YEAR("year", "Year"),
    NONE("none", "None"),
    ;

    fun next(): GalleryGroupingMode =
        entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStored(value: String?): GalleryGroupingMode =
            entries.firstOrNull { it.storedValue == value } ?: DAY
    }
}

enum class GallerySortPreference(
    val storedValue: String,
    val label: String,
    val mediaSortOrder: MediaSortOrder,
) {
    NEWEST("newest", "Newest first", MediaSortOrder.NEWEST),
    OLDEST("oldest", "Oldest first", MediaSortOrder.OLDEST),
    ;

    companion object {
        fun fromStored(value: String?): GallerySortPreference =
            entries.firstOrNull { it.storedValue == value } ?: NEWEST
    }
}

enum class GalleryViewDensity(
    val storedValue: String,
    val label: String,
    private val columnAdjustment: Int,
) {
    DENSE("dense", "Dense", 0),
    COMFORTABLE("comfortable", "Comfortable", -1),
    SPACIOUS("spacious", "Spacious", -2),
    ;

    fun mediaGridColumns(widthDp: Int): Int =
        (GalleryGlazeContract.gridColumns(widthDp) + columnAdjustment).coerceAtLeast(2)

    fun mediaGridColumnsForGroup(widthDp: Int, itemCount: Int): Int {
        val baseline = mediaGridColumns(widthDp)
        if (itemCount <= 0 || baseline <= 3) return baseline
        return if (itemCount < baseline) 3 else baseline
    }

    fun next(): GalleryViewDensity =
        entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStored(value: String?): GalleryViewDensity =
            entries.firstOrNull { it.storedValue == value } ?: DENSE
    }
}

enum class GallerySlideshowInterval(
    val storedValue: String,
    val label: String,
    val intervalMs: Long,
) {
    FAST("fast", "Every 3 seconds", 3_000L),
    NORMAL("normal", "Every 5 seconds", 5_000L),
    RELAXED("relaxed", "Every 10 seconds", 10_000L),
    ;

    companion object {
        fun fromStored(value: String?): GallerySlideshowInterval =
            entries.firstOrNull { it.storedValue == value } ?: NORMAL
    }
}

enum class GalleryVideoFilter(val label: String) {
    ALL("All"),
    SCREEN_RECORDINGS("Screen recordings"),
    CAMERA("Camera"),
    FAVORITES("Favorites"),
}

object GalleryVideoPresentationPolicy {
    fun orderLabel(sortOrder: MediaSortOrder): String = when (sortOrder) {
        MediaSortOrder.NEWEST -> "Recently added"
        MediaSortOrder.OLDEST -> "Oldest first"
    }

    fun filterAndOrderLabel(
        filter: GalleryVideoFilter,
        sortOrder: MediaSortOrder,
    ): String {
        val order = orderLabel(sortOrder)
        return if (filter == GalleryVideoFilter.ALL) order else "${filter.label} · $order"
    }
}

object GalleryVideoFilterPolicy {
    fun available(
        items: List<MediaItem>,
        favoriteContentUris: Set<String>,
    ): List<GalleryVideoFilter> = buildList {
        add(GalleryVideoFilter.ALL)
        GalleryVideoFilter.entries
            .filterNot { it == GalleryVideoFilter.ALL }
            .filter { filter -> items.any { item -> matches(filter, item, favoriteContentUris) } }
            .forEach { filter -> add(filter) }
    }

    fun filter(
        items: List<MediaItem>,
        selected: GalleryVideoFilter,
        favoriteContentUris: Set<String>,
    ): List<MediaItem> =
        if (selected == GalleryVideoFilter.ALL) items
        else items.filter { item -> matches(selected, item, favoriteContentUris) }

    fun matches(
        filter: GalleryVideoFilter,
        item: MediaItem,
        favoriteContentUris: Set<String>,
    ): Boolean = when (filter) {
        GalleryVideoFilter.ALL -> true
        GalleryVideoFilter.FAVORITES -> item.contentUri in favoriteContentUris
        GalleryVideoFilter.CAMERA ->
            item.albumName?.contains("camera", ignoreCase = true) == true
        GalleryVideoFilter.SCREEN_RECORDINGS -> {
            val searchable = listOfNotNull(item.albumName, item.displayName)
                .joinToString(" ")
                .lowercase()
            "screen" in searchable && ("record" in searchable || "capture" in searchable)
        }
    }
}

data class GalleryUserSettings(
    val fileLoadingPriority: GalleryFileLoadingPriority = GalleryFileLoadingPriority.FAST,
    val viewDensity: GalleryViewDensity = GalleryViewDensity.DENSE,
    val groupingMode: GalleryGroupingMode = GalleryGroupingMode.DAY,
    val sortPreference: GallerySortPreference = GallerySortPreference.NEWEST,
    val pinnedAlbumIds: Set<String> = emptySet(),
    val albumOrderIds: List<String> = emptyList(),
    val includedAlbumIds: Set<String> = emptySet(),
    val excludedAlbumIds: Set<String> = emptySet(),
    val showHiddenItems: Boolean = false,
    val playVideosAutomatically: Boolean = false,
    val loopVideos: Boolean = false,
    val loopSlideshows: Boolean = false,
    val slideshowInterval: GallerySlideshowInterval = GallerySlideshowInterval.NORMAL,
    val animateGifThumbnails: Boolean = false,
    val deleteEmptyFolders: Boolean = false,
    val moveDeletedItemsToRecycleBin: Boolean = true,
    val roundedSquareThumbnails: Boolean = true,
    val contextualHintsEnabled: Boolean = true,
)

object GallerySettingsPolicy {
    const val EXPORT_SCHEMA_VERSION = 1

    fun visibleItems(items: List<MediaItem>, settings: GalleryUserSettings): List<MediaItem> =
        items.filter { item ->
            val included = settings.includedAlbumIds.isEmpty() || item.albumId in settings.includedAlbumIds
            val excluded = item.albumId != null && item.albumId in settings.excludedAlbumIds
            val hidden = isHidden(item)
            included && !excluded && (settings.showHiddenItems || !hidden)
        }

    fun isHidden(item: MediaItem): Boolean =
        item.displayName.trimStart().startsWith('.') ||
            item.albumName?.trimStart()?.startsWith('.') == true
}
