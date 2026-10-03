package com.goreecloud.gallery

object GalleryLiveRefreshPolicy {
    fun shouldDefer(
        viewerOpen: Boolean,
        mediaMutationPending: Boolean,
        mediaMovePending: Boolean,
        mediaMoveExecutionInProgress: Boolean,
        mediaCopyExecutionInProgress: Boolean,
    ): Boolean =
        viewerOpen ||
            mediaMutationPending ||
            mediaMovePending ||
            mediaMoveExecutionInProgress ||
            mediaCopyExecutionInProgress
}
