package com.goreecloud.launcher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeApp
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeFolder
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePageManagerPolicyTest {
    private val primary = WorkspaceRenderedHomePage(
        pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
        rank = 0,
        appKeys = emptyList(),
        unsupportedItemCount = 0,
    )

    @Test
    fun emptySecondaryPageCanBeDeletedWhenLayoutUnlocked() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertTrue(
            canDeleteHomePage(
                page = secondary,
                pages = listOf(primary, secondary),
                layoutLocked = false,
            ),
        )
    }

    @Test
    fun primaryPageIsAlwaysProtected() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertFalse(
            canDeleteHomePage(
                page = primary,
                pages = listOf(primary, secondary),
                layoutLocked = false,
            ),
        )
    }

    @Test
    fun pageWithAppsCannotBeDeletedFromManager() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = listOf("com.example/.Main"),
            unsupportedItemCount = 0,
        )

        assertFalse(
            canDeleteHomePage(
                page = secondary,
                pages = listOf(primary, secondary),
                layoutLocked = false,
            ),
        )
    }

    @Test
    fun layoutLockDisablesPageDeletion() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertFalse(
            canDeleteHomePage(
                page = secondary,
                pages = listOf(primary, secondary),
                layoutLocked = true,
            ),
        )
    }

    @Test
    fun invalidPrimaryOrderingFailsClosed() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertFalse(
            canDeleteHomePage(
                page = secondary,
                pages = listOf(secondary, primary),
                layoutLocked = false,
            ),
        )
    }

    @Test
    fun horizontalHomeSwipeMovesBetweenAdjacentPages() {
        assertEquals(
            1,
            homePageSwipeTargetIndex(
                currentIndex = 0,
                pageCount = 3,
                horizontalDistancePx = -160f,
                verticalDistancePx = 12f,
                minimumDistancePx = 56f,
            ),
        )
        assertEquals(
            0,
            homePageSwipeTargetIndex(
                currentIndex = 1,
                pageCount = 3,
                horizontalDistancePx = 160f,
                verticalDistancePx = 12f,
                minimumDistancePx = 56f,
            ),
        )
    }

    @Test
    fun verticalOrEdgeSwipeDoesNotChangeHomePage() {
        assertNull(
            homePageSwipeTargetIndex(
                currentIndex = 0,
                pageCount = 3,
                horizontalDistancePx = 48f,
                verticalDistancePx = 4f,
                minimumDistancePx = 56f,
            ),
        )
        assertNull(
            homePageSwipeTargetIndex(
                currentIndex = 0,
                pageCount = 3,
                horizontalDistancePx = -70f,
                verticalDistancePx = 68f,
                minimumDistancePx = 56f,
            ),
        )
        assertNull(
            homePageSwipeTargetIndex(
                currentIndex = 0,
                pageCount = 3,
                horizontalDistancePx = 120f,
                verticalDistancePx = 0f,
                minimumDistancePx = 56f,
            ),
        )
    }

    @Test
    fun secondaryHomeMoveTargetsIncludePrimaryAndExcludeCurrentPage() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val tertiary = WorkspaceRenderedHomePage(
            pageId = "home:user:tertiary",
            rank = 2,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertEquals(
            listOf(primary.pageId, tertiary.pageId),
            homeMoveTargetPages(
                pages = listOf(primary, secondary, tertiary),
                currentPageId = secondary.pageId,
            ).map { it.pageId },
        )
    }

    @Test
    fun primaryHomeMoveTargetsIncludeOnlySecondaryPages() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val tertiary = WorkspaceRenderedHomePage(
            pageId = "home:user:tertiary",
            rank = 2,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertEquals(
            listOf(secondary.pageId, tertiary.pageId),
            homeMoveTargetPages(
                pages = listOf(primary, secondary, tertiary),
                currentPageId = primary.pageId,
            ).map { it.pageId },
        )
    }

    @Test
    fun secondarySpatialSlotIndexPreservesEmptyCellsAndRejectsInvalidCoordinates() {
        assertEquals(13, homePageSpatialSlotIndex(3, 2, columns = 5, rows = 6))
        assertEquals(0, homePageSpatialSlotIndex(0, 0, columns = 5, rows = 6))
        assertNull(homePageSpatialSlotIndex(null, 0, columns = 5, rows = 6))
        assertNull(homePageSpatialSlotIndex(5, 0, columns = 5, rows = 6))
        assertNull(homePageSpatialSlotIndex(0, 6, columns = 5, rows = 6))
        assertNull(homePageSpatialSlotIndex(0, 0, columns = 0, rows = 6))
    }

    @Test
    fun measuredHomeGridMapsExactCellsAndRejectsSpacingGaps() {
        val bounds = Rect(
            left = 10f,
            top = 20f,
            right = 440f,
            bottom = 300f,
        )

        assertEquals(
            2 to 1,
            homePageCellAtPoint(
                point = Offset(250f, 150f),
                surfaceBounds = bounds,
                columns = 4,
                rows = 3,
                horizontalSpacingPx = 10f,
                verticalSpacingPx = 20f,
                tileHeightPx = 80f,
            ),
        )
        assertNull(
            homePageCellAtPoint(
                point = Offset(115f, 60f),
                surfaceBounds = bounds,
                columns = 4,
                rows = 3,
                horizontalSpacingPx = 10f,
                verticalSpacingPx = 20f,
                tileHeightPx = 80f,
            ),
        )
        assertNull(
            homePageCellAtPoint(
                point = Offset(250f, 310f),
                surfaceBounds = bounds,
                columns = 4,
                rows = 3,
                horizontalSpacingPx = 10f,
                verticalSpacingPx = 20f,
                tileHeightPx = 80f,
            ),
        )
    }

    @Test
    fun edgeDropTargetsOnlyValidAdjacentHomePages() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val tertiary = WorkspaceRenderedHomePage(
            pageId = "home:user:tertiary",
            rank = 2,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val pages = listOf(primary, secondary, tertiary)

        assertEquals(
            secondary.pageId,
            homePageEdgeDropTargetPageId(
                pages = pages,
                currentPageId = primary.pageId,
                dropX = 975f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
        assertEquals(
            primary.pageId,
            homePageEdgeDropTargetPageId(
                pages = pages,
                currentPageId = secondary.pageId,
                dropX = 24f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
        assertEquals(
            tertiary.pageId,
            homePageEdgeDropTargetPageId(
                pages = pages,
                currentPageId = secondary.pageId,
                dropX = 980f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
        assertNull(
            homePageEdgeDropTargetPageId(
                pages = pages,
                currentPageId = primary.pageId,
                dropX = 20f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
        assertNull(
            homePageEdgeDropTargetPageId(
                pages = pages,
                currentPageId = tertiary.pageId,
                dropX = 980f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
        assertNull(
            homePageEdgeDropTargetPageId(
                pages = pages,
                currentPageId = secondary.pageId,
                dropX = 500f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
    }

    @Test
    fun edgeDropMapsToExactOppositeEdgeCellAndDropRow() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val tertiary = WorkspaceRenderedHomePage(
            pageId = "home:user:tertiary",
            rank = 2,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val pages = listOf(primary, secondary, tertiary)

        assertEquals(
            HomePageEdgeDropTarget(
                pageId = secondary.pageId,
                cellX = 0,
                cellY = 3,
            ),
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = primary.pageId,
                dropX = 980f,
                dropY = 350f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 5,
                rows = 6,
            ),
        )
        assertEquals(
            HomePageEdgeDropTarget(
                pageId = primary.pageId,
                cellX = 4,
                cellY = 1,
            ),
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = secondary.pageId,
                dropX = 20f,
                dropY = 150f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 5,
                rows = 6,
            ),
        )
        assertEquals(
            HomePageEdgeDropTarget(
                pageId = tertiary.pageId,
                cellX = 0,
                cellY = 5,
            ),
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = secondary.pageId,
                dropX = 980f,
                dropY = 599f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 5,
                rows = 6,
            ),
        )
    }

    @Test
    fun edgeDropRespectsMultiCellWidgetSpanAtOppositeEdgeAndBottomRow() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val pages = listOf(primary, secondary)

        assertEquals(
            HomePageEdgeDropTarget(
                pageId = primary.pageId,
                cellX = 3,
                cellY = 4,
            ),
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = secondary.pageId,
                dropX = 20f,
                dropY = 599f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 5,
                rows = 6,
                spanX = 2,
                spanY = 2,
            ),
        )
        assertNull(
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = primary.pageId,
                dropX = 980f,
                dropY = 300f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 5,
                rows = 6,
                spanX = 6,
                spanY = 1,
            ),
        )
    }

    @Test
    fun exactEdgeDropFailsClosedOutsideGridOrWithInvalidDimensions() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )
        val pages = listOf(primary, secondary)

        assertNull(
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = primary.pageId,
                dropX = 980f,
                dropY = 601f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 5,
                rows = 6,
            ),
        )
        assertNull(
            homePageEdgeDropTarget(
                pages = pages,
                currentPageId = primary.pageId,
                dropX = 980f,
                dropY = 300f,
                surfaceLeftPx = 0f,
                surfaceTopPx = 0f,
                surfaceRightPx = 1000f,
                surfaceBottomPx = 600f,
                edgeThresholdPx = 36f,
                columns = 0,
                rows = 6,
            ),
        )
    }

    @Test
    fun edgeDropFailsClosedForInvalidPageOrderingOrGeometry() {
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
        )

        assertNull(
            homePageEdgeDropTargetPageId(
                pages = listOf(secondary, primary),
                currentPageId = secondary.pageId,
                dropX = 20f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 1000f,
                edgeThresholdPx = 36f,
            ),
        )
        assertNull(
            homePageEdgeDropTargetPageId(
                pages = listOf(primary, secondary),
                currentPageId = primary.pageId,
                dropX = 990f,
                surfaceLeftPx = 0f,
                surfaceRightPx = 60f,
                edgeThresholdPx = 36f,
            ),
        )
    }


    @Test
    fun appGroupMoveActionRequiresTwoAvailablePositionedAppsOnUnlockedSecondaryPage() {
        val firstKey = "10:com.example.one/.Main"
        val secondKey = "10:com.example.two/.Main"
        val secondary = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = listOf(firstKey, secondKey),
            unsupportedItemCount = 0,
            appPlacements = listOf(
                WorkspaceRenderedHomeApp(firstKey, 0, 0, 1, 1),
                WorkspaceRenderedHomeApp(secondKey, 1, 0, 1, 1),
            ),
        )

        assertTrue(
            canMoveHomeAppGroup(
                page = secondary,
                layoutLocked = false,
                availableAppKeys = setOf(firstKey, secondKey),
            ),
        )
        assertFalse(
            canMoveHomeAppGroup(
                page = secondary,
                layoutLocked = true,
                availableAppKeys = setOf(firstKey, secondKey),
            ),
        )
        assertFalse(
            canMoveHomeAppGroup(
                page = secondary,
                layoutLocked = false,
                availableAppKeys = setOf(firstKey),
            ),
        )
        assertFalse(
            canMoveHomeAppGroup(
                page = primary.copy(
                    appKeys = listOf(firstKey, secondKey),
                    appPlacements = listOf(
                        WorkspaceRenderedHomeApp(firstKey, 0, 0, 1, 1),
                        WorkspaceRenderedHomeApp(secondKey, 1, 0, 1, 1),
                    ),
                ),
                layoutLocked = false,
                availableAppKeys = setOf(firstKey, secondKey),
            ),
        )
        assertFalse(
            canMoveHomeAppGroup(
                page = secondary.copy(
                    appKeys = listOf(firstKey, firstKey, secondKey),
                    appPlacements = listOf(
                        WorkspaceRenderedHomeApp(firstKey, 0, 0, 1, 1),
                        WorkspaceRenderedHomeApp(firstKey, 0, 1, 1, 1),
                        WorkspaceRenderedHomeApp(secondKey, 1, 0, 1, 1),
                    ),
                ),
                layoutLocked = false,
                availableAppKeys = setOf(firstKey, secondKey),
            ),
        )
    }


    @Test
    fun groupAnchorPreflightPreservesRigidGeometryAndRejectsOccupiedOrOutOfBoundsCells() {
        val firstKey = "10:com.example.one/.Main"
        val secondKey = "10:com.example.two/.Main"
        val source = WorkspaceRenderedHomePage(
            pageId = "home:user:source",
            rank = 1,
            appKeys = listOf(firstKey, secondKey),
            unsupportedItemCount = 0,
            appPlacements = listOf(
                WorkspaceRenderedHomeApp(firstKey, 0, 0, 1, 1),
                WorkspaceRenderedHomeApp(secondKey, 1, 1, 1, 1),
            ),
        )
        val target = WorkspaceRenderedHomePage(
            pageId = "home:user:target",
            rank = 2,
            appKeys = emptyList(),
            unsupportedItemCount = 0,
            folderPlacements = listOf(
                WorkspaceRenderedHomeFolder(
                    itemId = "folder:item",
                    folderId = "folder:id",
                    cellX = 0,
                    cellY = 0,
                ),
            ),
        )

        assertFalse(
            homeAppGroupAnchorAvailable(
                sourcePage = source,
                targetPage = target,
                selectedAppKeys = listOf(firstKey, secondKey),
                columns = 4,
                rows = 5,
                targetCellX = 0,
                targetCellY = 0,
            ),
        )
        assertTrue(
            homeAppGroupAnchorAvailable(
                sourcePage = source,
                targetPage = target,
                selectedAppKeys = listOf(firstKey, secondKey),
                columns = 4,
                rows = 5,
                targetCellX = 1,
                targetCellY = 2,
            ),
        )
        assertFalse(
            homeAppGroupAnchorAvailable(
                sourcePage = source,
                targetPage = target,
                selectedAppKeys = listOf(firstKey, secondKey),
                columns = 4,
                rows = 5,
                targetCellX = 3,
                targetCellY = 4,
            ),
        )
    }

    @Test
    fun groupAnchorPreflightFailsClosedWhenTargetAppPlacementIsUnresolved() {
        val firstKey = "10:com.example.one/.Main"
        val secondKey = "10:com.example.two/.Main"
        val source = WorkspaceRenderedHomePage(
            pageId = "home:user:source",
            rank = 1,
            appKeys = listOf(firstKey, secondKey),
            unsupportedItemCount = 0,
            appPlacements = listOf(
                WorkspaceRenderedHomeApp(firstKey, 0, 0, 1, 1),
                WorkspaceRenderedHomeApp(secondKey, 1, 0, 1, 1),
            ),
        )
        val target = WorkspaceRenderedHomePage(
            pageId = "home:user:target",
            rank = 2,
            appKeys = listOf("10:com.example.unresolved/.Main"),
            unsupportedItemCount = 0,
            appPlacements = listOf(
                WorkspaceRenderedHomeApp(
                    "10:com.example.unresolved/.Main",
                    null,
                    null,
                    1,
                    1,
                ),
            ),
        )

        assertFalse(
            homeAppGroupAnchorAvailable(
                sourcePage = source,
                targetPage = target,
                selectedAppKeys = listOf(firstKey, secondKey),
                columns = 4,
                rows = 5,
                targetCellX = 1,
                targetCellY = 2,
            ),
        )
    }

    @Test
    fun samePageGroupAnchorPreflightIgnoresSelectedSourceCellsButKeepsOtherOccupancy() {
        val firstKey = "10:com.example.one/.Main"
        val secondKey = "10:com.example.two/.Main"
        val thirdKey = "10:com.example.three/.Main"
        val page = WorkspaceRenderedHomePage(
            pageId = "home:user:secondary",
            rank = 1,
            appKeys = listOf(firstKey, secondKey, thirdKey),
            unsupportedItemCount = 0,
            appPlacements = listOf(
                WorkspaceRenderedHomeApp(firstKey, 0, 0, 1, 1),
                WorkspaceRenderedHomeApp(secondKey, 1, 0, 1, 1),
                WorkspaceRenderedHomeApp(thirdKey, 2, 0, 1, 1),
            ),
        )

        assertTrue(
            homeAppGroupAnchorAvailable(
                sourcePage = page,
                targetPage = page,
                selectedAppKeys = listOf(firstKey, secondKey),
                columns = 4,
                rows = 5,
                targetCellX = 0,
                targetCellY = 1,
            ),
        )
        assertFalse(
            homeAppGroupAnchorAvailable(
                sourcePage = page,
                targetPage = page,
                selectedAppKeys = listOf(firstKey, secondKey),
                columns = 4,
                rows = 5,
                targetCellX = 1,
                targetCellY = 0,
            ),
        )
    }

    @Test
    fun singleHomePageCannotBeDeleted() {
        assertFalse(
            canDeleteHomePage(
                page = primary,
                pages = listOf(primary),
                layoutLocked = false,
            ),
        )
    }
}
