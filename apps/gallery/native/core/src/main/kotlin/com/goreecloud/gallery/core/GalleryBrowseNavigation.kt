package com.goreecloud.gallery.core

enum class GalleryBrowseSection {
    PICTURES,
    ALBUMS,
}

data class GalleryBrowseDestination(
    val section: GalleryBrowseSection,
    val label: String,
    val itemCount: Int,
    val selected: Boolean,
)

data class GalleryBrowseNavigation(
    val selectedSection: GalleryBrowseSection,
    val destinations: List<GalleryBrowseDestination>,
)

/**
 * Builds the first-party Pictures/Albums information-architecture projection from
 * the currently authorized local media only. It does not introduce a second media
 * authority, synthetic albums, network discovery, or cloud state.
 */
fun buildGalleryBrowseNavigation(
    authorizedItems: List<MediaItem>,
    selectedSection: GalleryBrowseSection,
): GalleryBrowseNavigation {
    val albums = authorizedItems.buildAlbumCatalog()
    return GalleryBrowseNavigation(
        selectedSection = selectedSection,
        destinations = listOf(
            GalleryBrowseDestination(
                section = GalleryBrowseSection.PICTURES,
                label = "Pictures",
                itemCount = authorizedItems.size,
                selected = selectedSection == GalleryBrowseSection.PICTURES,
            ),
            GalleryBrowseDestination(
                section = GalleryBrowseSection.ALBUMS,
                label = "Albums",
                itemCount = albums.size,
                selected = selectedSection == GalleryBrowseSection.ALBUMS,
            ),
        ),
    )
}

fun GalleryBrowseSection.next(): GalleryBrowseSection = when (this) {
    GalleryBrowseSection.PICTURES -> GalleryBrowseSection.ALBUMS
    GalleryBrowseSection.ALBUMS -> GalleryBrowseSection.PICTURES
}
