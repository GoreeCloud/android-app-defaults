package com.goreecloud.gallery.android

import android.provider.MediaStore
import java.net.URI

/**
 * Shared canonical Android MediaStore image/video item URI boundary.
 *
 * Browsing, playback, trash, restore, and deletion may share this exact identity
 * rule without turning a rendered Gallery item into filesystem or arbitrary
 * ContentProvider authority.
 */
object AndroidMediaStoreItemUriPolicy {
    const val MAX_CONTENT_URI_CHARACTERS = 1024

    private val canonicalItemPath =
        Regex("^/([A-Za-z0-9_-]+)/(images|video)/media/([1-9][0-9]*)$")

    fun requireCanonicalItemUri(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "media content URI must not be blank" }
        require(value == raw) { "MediaStore item URI must already use its exact canonical form" }
        require(value.length <= MAX_CONTENT_URI_CHARACTERS) {
            "MediaStore item URI exceeds the supported size bound"
        }

        val parsed = try {
            URI(value)
        } catch (error: Exception) {
            throw IllegalArgumentException("invalid media content URI", error)
        }
        require(parsed.scheme == "content") { "only content URIs are supported" }
        require(parsed.authority == MediaStore.AUTHORITY) { "only Android MediaStore URIs are supported" }
        require(parsed.rawQuery == null && parsed.rawFragment == null) {
            "MediaStore item URI must not include query parameters or fragments"
        }
        require(parsed.userInfo == null && parsed.port == -1) {
            "MediaStore item URI must use the canonical content authority form"
        }
        require(parsed.rawPath == parsed.path && '\\' !in parsed.rawPath.orEmpty()) {
            "MediaStore item paths must not use encoded or backslash path controls"
        }

        val match = canonicalItemPath.matchEntire(parsed.rawPath.orEmpty())
        require(match != null) {
            "MediaStore item URI must use /<volume>/(images|video)/media/<positive canonical id>"
        }
        require(match.groupValues[3].toLongOrNull() != null) {
            "MediaStore item URI ID exceeds the supported numeric range"
        }
        return value
    }

    fun isCanonicalItemUri(raw: String): Boolean =
        try {
            requireCanonicalItemUri(raw)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
}
