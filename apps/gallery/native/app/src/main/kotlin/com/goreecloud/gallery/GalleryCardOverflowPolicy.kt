package com.goreecloud.gallery

enum class GalleryCardOverflowAction(val label: String) {
    OPEN("Open"),
    SHARE("Share"),
    ADD_FAVORITE("Add to Favorites"),
    REMOVE_FAVORITE("Remove from Favorites"),
    DETAILS("Details"),
}

object GalleryCardOverflowPolicy {
    fun albumActions(): List<GalleryCardOverflowAction> =
        listOf(
            GalleryCardOverflowAction.OPEN,
            GalleryCardOverflowAction.DETAILS,
        )

    fun videoActions(isFavorite: Boolean): List<GalleryCardOverflowAction> =
        listOf(
            GalleryCardOverflowAction.SHARE,
            if (isFavorite) GalleryCardOverflowAction.REMOVE_FAVORITE else GalleryCardOverflowAction.ADD_FAVORITE,
            GalleryCardOverflowAction.DETAILS,
        )
}
