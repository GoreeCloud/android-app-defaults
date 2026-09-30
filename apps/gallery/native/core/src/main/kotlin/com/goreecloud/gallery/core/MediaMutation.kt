package com.goreecloud.gallery.core

enum class MutationKind { DELETE, MOVE }

data class MediaMutation(
    val kind: MutationKind,
    val mediaIds: Set<String>,
    val destinationAlbumId: String? = null,
    val permanent: Boolean = false,
) {
    init {
        require(mediaIds.isNotEmpty()) { "at least one media id is required" }
        require(mediaIds.none { it.isBlank() }) { "media ids must not be blank" }
        if (kind == MutationKind.MOVE) {
            require(!destinationAlbumId.isNullOrBlank()) { "move operations require a destination album" }
            require(!permanent) { "move operations cannot be permanent" }
        }
        if (kind == MutationKind.DELETE) {
            require(destinationAlbumId == null) { "delete operations cannot specify a destination album" }
        }
    }
}

sealed interface MutationDecision {
    data class Allowed(val requiresConfirmation: Boolean) : MutationDecision
    data class Denied(val reason: String) : MutationDecision
}

object MediaMutationPolicy {
    fun evaluate(mutation: MediaMutation, hasPlatformWriteGrant: Boolean): MutationDecision {
        if (!hasPlatformWriteGrant) {
            return MutationDecision.Denied("platform-authorized media write access is required")
        }
        return when (mutation.kind) {
            MutationKind.DELETE -> MutationDecision.Allowed(requiresConfirmation = true)
            MutationKind.MOVE -> MutationDecision.Allowed(requiresConfirmation = false)
        }
    }
}
