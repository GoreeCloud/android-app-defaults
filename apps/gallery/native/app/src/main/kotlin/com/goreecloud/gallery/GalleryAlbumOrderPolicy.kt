package com.goreecloud.gallery

internal enum class GalleryAlbumMoveDirection {
    EARLIER,
    LATER,
}

/**
 * App-local ordering policy for ordinary Albums collections.
 *
 * Provider-owned album identifiers remain the only identities. Stored order entries that are not
 * in the current authorized snapshot are ignored for rendering but preserved when an active item is
 * moved so a temporarily unavailable album is not silently erased from the user's local preference.
 * Pinned albums remain a separate leading section; manual movement never crosses that boundary.
 */
internal object GalleryAlbumOrderPolicy {
    const val MAX_ORDER_IDS = 500

    fun orderedIds(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
        manualOrderIds: List<String>,
    ): List<String> {
        val available = normalizedIds(availableAlbumIds)
        if (available.isEmpty()) return emptyList()

        val availableSet = available.toHashSet()
        val storedVisible = normalizedIds(manualOrderIds).filter { it in availableSet }
        val storedVisibleSet = storedVisible.toHashSet()
        val base = storedVisible + available.filterNot { it in storedVisibleSet }

        val pinned = base.filter { it in pinnedAlbumIds }
        val unpinned = base.filterNot { it in pinnedAlbumIds }
        return pinned + unpinned
    }

    fun canMove(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
        manualOrderIds: List<String>,
        albumId: String,
        direction: GalleryAlbumMoveDirection,
    ): Boolean {
        val normalizedId = albumId.trim()
        if (normalizedId.isEmpty()) return false
        val ordered = orderedIds(availableAlbumIds, pinnedAlbumIds, manualOrderIds)
        val group = ordered.filter { (it in pinnedAlbumIds) == (normalizedId in pinnedAlbumIds) }
        val index = group.indexOf(normalizedId)
        if (index < 0) return false
        return when (direction) {
            GalleryAlbumMoveDirection.EARLIER -> index > 0
            GalleryAlbumMoveDirection.LATER -> index < group.lastIndex
        }
    }

    fun movedOrder(
        availableAlbumIds: List<String>,
        pinnedAlbumIds: Set<String>,
        manualOrderIds: List<String>,
        albumId: String,
        direction: GalleryAlbumMoveDirection,
    ): List<String> {
        val normalizedId = albumId.trim()
        require(normalizedId.isNotEmpty()) { "album id is required" }

        val active = orderedIds(availableAlbumIds, pinnedAlbumIds, manualOrderIds).toMutableList()
        require(normalizedId in active) { "album must exist in the current authorized snapshot" }

        val samePinStateIndices = active.indices.filter { index ->
            (active[index] in pinnedAlbumIds) == (normalizedId in pinnedAlbumIds)
        }
        val activeIndex = active.indexOf(normalizedId)
        val groupIndex = samePinStateIndices.indexOf(activeIndex)
        val targetGroupIndex = when (direction) {
            GalleryAlbumMoveDirection.EARLIER -> groupIndex - 1
            GalleryAlbumMoveDirection.LATER -> groupIndex + 1
        }
        if (targetGroupIndex !in samePinStateIndices.indices) {
            return preserveUnavailable(active, availableAlbumIds, manualOrderIds)
        }

        val targetIndex = samePinStateIndices[targetGroupIndex]
        val swap = active[targetIndex]
        active[targetIndex] = normalizedId
        active[activeIndex] = swap
        return preserveUnavailable(active, availableAlbumIds, manualOrderIds)
    }

    private fun preserveUnavailable(
        activeOrder: List<String>,
        availableAlbumIds: List<String>,
        manualOrderIds: List<String>,
    ): List<String> {
        val available = normalizedIds(availableAlbumIds).toHashSet()
        val unavailableStored = normalizedIds(manualOrderIds).filterNot { it in available }
        return activeOrder + unavailableStored
    }

    private fun normalizedIds(values: Collection<String>): List<String> =
        values.asSequence()
            .map { it.trim() }
            .filter(String::isNotEmpty)
            .distinct()
            .take(MAX_ORDER_IDS)
            .toList()
}
