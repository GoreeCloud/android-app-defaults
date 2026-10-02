package com.goreecloud.gallery

enum class GalleryAlbumMoveDirection {
    EARLIER,
    LATER,
}

data class GalleryAlbumMoveAvailability(
    val canMoveEarlier: Boolean,
    val canMoveLater: Boolean,
)

internal object GalleryAlbumOrderPolicy {
    fun orderedIds(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
        preferredAlbumOrderIds: List<String>,
    ): List<String> {
        val available = availableAlbumIds
            .filter(String::isNotBlank)
            .distinct()
        if (available.isEmpty()) return emptyList()

        val availableSet = available.toSet()
        val preferredVisible = preferredAlbumOrderIds
            .filter { it.isNotBlank() && it in availableSet }
            .distinct()
        val preferredSet = preferredVisible.toSet()
        val base = preferredVisible + available.filterNot { it in preferredSet }

        val pinned = base.filter { it in pinnedAlbumIds }
        val unpinned = base.filterNot { it in pinnedAlbumIds }
        return pinned + unpinned
    }

    fun availability(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
        preferredAlbumOrderIds: List<String>,
        albumId: String,
    ): GalleryAlbumMoveAvailability {
        val ordered = orderedIds(availableAlbumIds, pinnedAlbumIds, preferredAlbumOrderIds)
        val group = ordered.filter { (it in pinnedAlbumIds) == (albumId in pinnedAlbumIds) }
        val index = group.indexOf(albumId)
        if (index < 0) {
            return GalleryAlbumMoveAvailability(
                canMoveEarlier = false,
                canMoveLater = false,
            )
        }
        return GalleryAlbumMoveAvailability(
            canMoveEarlier = index > 0,
            canMoveLater = index < group.lastIndex,
        )
    }

    fun moved(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
        preferredAlbumOrderIds: List<String>,
        albumId: String,
        direction: GalleryAlbumMoveDirection,
    ): List<String> {
        require(albumId.isNotBlank()) { "album id is required" }

        val ordered = orderedIds(availableAlbumIds, pinnedAlbumIds, preferredAlbumOrderIds)
        require(albumId in ordered) { "album must be currently available" }

        val samePinnedState = albumId in pinnedAlbumIds
        val group = ordered.filter { (it in pinnedAlbumIds) == samePinnedState }
        val groupIndex = group.indexOf(albumId)
        val targetGroupIndex = when (direction) {
            GalleryAlbumMoveDirection.EARLIER -> groupIndex - 1
            GalleryAlbumMoveDirection.LATER -> groupIndex + 1
        }
        if (targetGroupIndex !in group.indices) return preferredAlbumOrderIds.distinct()

        val targetId = group[targetGroupIndex]
        val mutable = ordered.toMutableList()
        val from = mutable.indexOf(albumId)
        val to = mutable.indexOf(targetId)
        val displaced = mutable[to]
        mutable[to] = mutable[from]
        mutable[from] = displaced

        val availableSet = availableAlbumIds.toSet()
        val stale = preferredAlbumOrderIds
            .filter { it.isNotBlank() && it !in availableSet }
            .distinct()
        return mutable + stale
    }
}
