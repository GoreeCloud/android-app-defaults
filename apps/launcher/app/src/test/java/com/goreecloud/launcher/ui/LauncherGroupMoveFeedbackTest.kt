package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.workspace.db.WorkspacePagedRoomMutationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherGroupMoveFeedbackTest {
    @Test
    fun successReportsMovedAppCount() {
        assertEquals(
            "Moved 2 apps.",
            launcherGroupMoveFeedback(
                WorkspacePagedRoomMutationResult.UpdatedItems(
                    items = listOf(
                        WorkspacePagedRoomMutationResult.UpdatedItem(
                            itemId = "one",
                            pageId = "home:1",
                            cellX = 0,
                            cellY = 0,
                            spanX = 1,
                            spanY = 1,
                        ),
                        WorkspacePagedRoomMutationResult.UpdatedItem(
                            itemId = "two",
                            pageId = "home:1",
                            cellX = 1,
                            cellY = 0,
                            spanX = 1,
                            spanY = 1,
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun staleWorkspaceExplainsThatTheLayoutChanged() {
        assertEquals(
            "Home changed while you were editing. Review the current layout and try the move again.",
            launcherGroupMoveFeedback(WorkspacePagedRoomMutationResult.StoredWorkspaceChanged),
        )
    }

    @Test
    fun invalidPlacementExplainsHowToRecover() {
        assertEquals(
            "That group does not fit at this anchor. Choose another free destination.",
            launcherGroupMoveFeedback(WorkspacePagedRoomMutationResult.InvalidWorkspace),
        )
    }

    @Test
    fun changedSelectionRequiresReselection() {
        assertEquals(
            "One of the selected apps changed or is no longer on this page. Reopen Move apps and select the group again.",
            launcherGroupMoveFeedback(WorkspacePagedRoomMutationResult.ItemIdentityMismatch),
        )
    }

    @Test
    fun primaryHomeBoundaryIsExplained() {
        assertEquals(
            "Move apps together currently supports secondary Home pages only.",
            launcherGroupMoveFeedback(WorkspacePagedRoomMutationResult.PrimaryPageProtected),
        )
    }

    @Test
    fun unexpectedMutationResultRemainsFailClosed() {
        assertEquals(
            "Group move could not be applied. Your current Home layout was preserved.",
            launcherGroupMoveFeedback(
                WorkspacePagedRoomMutationResult.Updated(
                    orderedPageIds = listOf("home:0", "home:1"),
                ),
            ),
        )
    }
}
