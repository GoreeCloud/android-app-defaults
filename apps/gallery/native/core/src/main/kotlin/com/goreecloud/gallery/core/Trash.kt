package com.goreecloud.gallery.core

import java.time.Instant

data class TrashEntry(
    val mediaId: String,
    val trashedAt: Instant,
    val originalAlbumId: String? = null,
) {
    init {
        require(mediaId.isNotBlank()) { "media id is required" }
    }
}

enum class TrashAction { RESTORE, PURGE }

sealed interface TrashDecision {
    data object Allowed : TrashDecision
    data class Denied(val reason: String) : TrashDecision
}

object TrashPolicy {
    fun evaluate(action: TrashAction, hasPlatformWriteGrant: Boolean, confirmed: Boolean): TrashDecision {
        if (!hasPlatformWriteGrant) {
            return TrashDecision.Denied("platform-authorized media write access is required")
        }
        if (action == TrashAction.PURGE && !confirmed) {
            return TrashDecision.Denied("permanent purge requires explicit confirmation")
        }
        return TrashDecision.Allowed
    }
}
