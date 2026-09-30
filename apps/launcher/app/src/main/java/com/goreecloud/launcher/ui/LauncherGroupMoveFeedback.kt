package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.workspace.db.WorkspacePagedRoomMutationResult

internal fun launcherGroupMoveFeedback(
    result: WorkspacePagedRoomMutationResult,
): String =
    when (result) {
        is WorkspacePagedRoomMutationResult.UpdatedItems ->
            "Moved ${result.items.size} apps."
        WorkspacePagedRoomMutationResult.StoredWorkspaceChanged,
        WorkspacePagedRoomMutationResult.StoredPageSetChanged ->
            "Home changed while you were editing. Review the current layout and try the move again."
        WorkspacePagedRoomMutationResult.ItemNotFound,
        WorkspacePagedRoomMutationResult.ItemIdentityMismatch ->
            "One of the selected apps changed or is no longer on this page. Reopen Move apps and select the group again."
        WorkspacePagedRoomMutationResult.PageNotFound ->
            "That Home page is no longer available. Reopen Move apps and choose a current page."
        WorkspacePagedRoomMutationResult.PrimaryPageProtected ->
            "Move apps together currently supports secondary Home pages only."
        WorkspacePagedRoomMutationResult.InvalidWorkspace,
        WorkspacePagedRoomMutationResult.TargetRankOutOfRange ->
            "That group does not fit at this anchor. Choose another free destination."
        WorkspacePagedRoomMutationResult.Reserved,
        WorkspacePagedRoomMutationResult.Unavailable ->
            "Home storage is not available for group editing right now. Try again."
        else ->
            "Group move could not be applied. Your current Home layout was preserved."
    }
