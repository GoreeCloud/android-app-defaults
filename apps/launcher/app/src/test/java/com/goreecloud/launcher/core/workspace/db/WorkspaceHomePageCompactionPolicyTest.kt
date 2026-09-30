package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceHomePageCompactionPolicyTest {
    private val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5)

    @Test
    fun compactionPreservesRankOrderAndPacksRowMajor() {
        val pageId = "home:user:two"
        val items = listOf(
            appItem("b", pageId, rank = 4, x = 3, y = 3),
            appItem("a", pageId, rank = 1, x = 2, y = 2),
            appItem("c", pageId, rank = 8, x = 1, y = 4),
        )

        val plan = WorkspaceHomePageCompactionPolicy.plan(grid, pageId, items)
        assertTrue(plan is WorkspaceHomePageCompactionPolicy.Plan.Updated)
        val compacted = (plan as WorkspaceHomePageCompactionPolicy.Plan.Updated).items

        assertEquals(listOf("a", "b", "c"), compacted.map { it.itemId })
        assertEquals(listOf(0, 1, 2), compacted.map { it.rank })
        assertEquals(listOf(0, 1, 2), compacted.map { it.cellX })
        assertEquals(listOf(0, 0, 0), compacted.map { it.cellY })
    }

    @Test
    fun compactionWrapsAcrossRowsDeterministically() {
        val pageId = "home:user:two"
        val items = (0 until 6).map { index ->
            appItem("item-$index", pageId, rank = index, x = 3 - (index % 4), y = 4)
        }

        val compacted = (WorkspaceHomePageCompactionPolicy.plan(grid, pageId, items)
            as WorkspaceHomePageCompactionPolicy.Plan.Updated).items

        assertEquals(listOf(0, 1, 2, 3, 0, 1), compacted.map { it.cellX })
        assertEquals(listOf(0, 0, 0, 0, 1, 1), compacted.map { it.cellY })
    }

    @Test
    fun unsafeScopesFailClosed() {
        val pageId = "home:user:two"
        val unsupported = appItem("folder", pageId, 0, 0, 0).copy(
            itemType = WorkspaceItemType.FOLDER,
            appKey = null,
        )
        val spanning = appItem("wide", pageId, 1, 2, 2).copy(spanX = 2)

        assertEquals(
            WorkspaceHomePageCompactionPolicy.Plan.PrimaryPageProtected,
            WorkspaceHomePageCompactionPolicy.plan(
                grid,
                WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                listOf(appItem("primary", WorkspaceLegacyImportMapper.HOME_PAGE_ID, 0, 0, 0)),
            ),
        )
        assertEquals(
            WorkspaceHomePageCompactionPolicy.Plan.UnsupportedPageItems,
            WorkspaceHomePageCompactionPolicy.plan(grid, pageId, listOf(unsupported)),
        )
        assertEquals(
            WorkspaceHomePageCompactionPolicy.Plan.UnsupportedPageItems,
            WorkspaceHomePageCompactionPolicy.plan(grid, pageId, listOf(spanning)),
        )
        assertEquals(
            WorkspaceHomePageCompactionPolicy.Plan.InvalidPage,
            WorkspaceHomePageCompactionPolicy.plan(grid, "", emptyList()),
        )
    }

    @Test
    fun capacityOverflowFailsClosedWithoutCrossPageSpill() {
        val smallGrid = WorkspaceGridPlacement.Grid(columns = 2, rows = 2)
        val pageId = "home:user:two"
        val items = (0 until 5).map { index -> appItem("item-$index", pageId, index, 0, 0) }

        assertEquals(
            WorkspaceHomePageCompactionPolicy.Plan.CapacityExceeded,
            WorkspaceHomePageCompactionPolicy.plan(smallGrid, pageId, items),
        )
    }

    @Test
    fun alreadyCompactedPageIsNoOp() {
        val pageId = "home:user:two"
        val items = listOf(
            appItem("a", pageId, 0, 0, 0),
            appItem("b", pageId, 1, 1, 0),
            appItem("c", pageId, 2, 2, 0),
        )
        assertEquals(
            WorkspaceHomePageCompactionPolicy.Plan.AlreadyCompact,
            WorkspaceHomePageCompactionPolicy.plan(grid, pageId, items),
        )
    }

    private fun appItem(
        id: String,
        pageId: String,
        rank: Int,
        x: Int,
        y: Int,
    ): WorkspaceItemEntity = WorkspaceItemEntity(
        itemId = id,
        pageId = pageId,
        itemType = WorkspaceItemType.APP,
        appKey = "pkg/$id",
        rank = rank,
        cellX = x,
        cellY = y,
        spanX = 1,
        spanY = 1,
    )
}
