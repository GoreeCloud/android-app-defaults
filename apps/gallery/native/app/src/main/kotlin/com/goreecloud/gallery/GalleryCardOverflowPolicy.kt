package com.goreecloud.gallery

enum class GalleryCardOverflowAction(val label: String) {
    OPEN("Open"),
    PIN_TO_TOP("Pin to top"),
    UNPIN_FROM_TOP("Unpin from top"),
    MOVE_EARLIER("Move earlier"),
    MOVE_LATER("Move later"),
    SHARE("Share"),
    ADD_FAVORITE("Add to Favorites"),
    REMOVE_FAVORITE("Remove from Favorites"),
    DETAILS("Details"),
}

object GalleryCardOverflowPolicy {
    fun albumActions(
        isPinned: Boolean,
        canPin: Boolean,
        canMoveEarlier: Boolean = false,
        canMoveLater: Boolean = false,
    ): List<GalleryCardOverflowAction> = buildList {
        add(GalleryCardOverflowAction.OPEN)
        if (canPin) {
            add(
                if (isPinned) GalleryCardOverflowAction.UNPIN_FROM_TOP
                else GalleryCardOverflowAction.PIN_TO_TOP,
            )
            if (canMoveEarlier) add(GalleryCardOverflowAction.MOVE_EARLIER)
            if (canMoveLater) add(GalleryCardOverflowAction.MOVE_LATER)
        }
        add(GalleryCardOverflowAction.DETAILS)
    }

    fun videoActions(isFavorite: Boolean): List<GalleryCardOverflowAction> =
        listOf(
            GalleryCardOverflowAction.SHARE,
            if (isFavorite) GalleryCardOverflowAction.REMOVE_FAVORITE else GalleryCardOverflowAction.ADD_FAVORITE,
            GalleryCardOverflowAction.DETAILS,
        )
}
