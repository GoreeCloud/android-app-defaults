package com.goreecloud.gallery

enum class GalleryViewerScaleMode {
    FIT,
    FILL,
    ;

    fun next(): GalleryViewerScaleMode = when (this) {
        FIT -> FILL
        FILL -> FIT
    }
}
