package com.goreecloud.gallery

/**
 * Stable in-app wire names used when switching between Gallery's primary destinations.
 *
 * Trash remains a dedicated Activity because Android MediaStore Trash has a separate mutation
 * authority and lifecycle surface. The shared extra lets the Trash tab return to an existing
 * GalleryActivity without inventing a second navigation state owner.
 */
internal object GalleryNavigationContract {
    const val EXTRA_DESTINATION = "com.goreecloud.gallery.extra.DESTINATION"

    const val PHOTOS = "photos"
    const val ALBUMS = "albums"
    const val VIDEOS = "videos"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
}
