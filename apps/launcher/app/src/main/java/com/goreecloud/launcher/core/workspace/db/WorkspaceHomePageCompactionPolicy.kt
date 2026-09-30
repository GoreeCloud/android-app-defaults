package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement

/**
 * Pure planning authority for bounded secondary-HOME page compaction.
 *
 * This foundation never touches the canonical primary HOME page, never interprets unsupported
 * item types, never resizes items, and never spills items to another page. Eligible 1x1 apps keep
 * authoritative rank order and are packed into deterministic row-major cells.
 */
object WorkspaceHomePageCompactionPolicy {
    sealed interface Plan {
        data class Updated(val items: List<WorkspaceItemEntity>) : Plan
        data object AlreadyCompact : Plan
        data object PrimaryPageProtected : Plan
        data object UnsupportedPageItems : Plan
        data object CapacityExceeded : Plan
        data object InvalidPage : Plan
    }

    fun plan(
        grid: WorkspaceGridPlacement.Grid,
        pageId: String,
        items: List<WorkspaceItemEntity>,
    ): Plan {
        if (pageId.isBlank()) return Plan.InvalidPage
        if (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
            return Plan.PrimaryPageProtected
        }

        val pageItems = items.filter { it.pageId == pageId }
        if (pageItems.isEmpty()) return Plan.AlreadyCompact
        if (
            pageItems.any {
                it.itemType != WorkspaceItemType.APP ||
                    it.appKey.isNullOrBlank() ||
                    it.cellX == null ||
                    it.cellY == null ||
                    it.spanX != 1 ||
                    it.spanY != 1
            }
        ) {
            return Plan.UnsupportedPageItems
        }

        val capacity = grid.columns.toLong() * grid.rows.toLong()
        if (pageItems.size.toLong() > capacity) return Plan.CapacityExceeded

        val ordered = pageItems.sortedWith(compareBy({ it.rank }, { it.itemId }))
        val compacted = ordered.mapIndexed { index, item ->
            item.copy(
                rank = index,
                cellX = index % grid.columns,
                cellY = index / grid.columns,
            )
        }

        return if (compacted == ordered) Plan.AlreadyCompact else Plan.Updated(compacted)
    }
}
