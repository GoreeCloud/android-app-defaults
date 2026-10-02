package com.goreecloud.gallery

internal data class GallerySelectionMenuState(
    val primaryActionLabel: String,
    val showDetails: Boolean,
)

internal object GallerySelectionMenuPolicy {
    fun state(
        visibleContentUris: List<String>,
        selectedContentUris: Set<String>,
    ): GallerySelectionMenuState {
        val visible = visibleContentUris.filter(String::isNotBlank).distinct()
        val allVisibleSelected =
            visible.isNotEmpty() &&
                selectedContentUris.size == visible.size &&
                visible.all(selectedContentUris::contains)
        return GallerySelectionMenuState(
            primaryActionLabel = if (allVisibleSelected) "Clear selection" else "Select all visible",
            showDetails = selectedContentUris.size == 1,
        )
    }
}
