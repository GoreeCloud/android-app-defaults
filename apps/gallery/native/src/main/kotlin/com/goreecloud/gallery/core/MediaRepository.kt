package com.goreecloud.gallery.core

/**
 * First-party GoreeCloud Gallery media boundary.
 *
 * The native Gallery application treats Android-owned media as authoritative and keeps
 * browsing usable without a server, account, or network connection. Photos integration
 * belongs behind optional adapters and must never become a requirement for local browsing.
 */
data class MediaItem(
    val id: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String,
    val width: Int?,
    val height: Int?,
    val durationMillis: Long?,
    val dateTakenEpochMillis: Long?,
    val relativePath: String?,
)

data class MediaQuery(
    val albumId: String? = null,
    val search: String? = null,
    val includeImages: Boolean = true,
    val includeVideos: Boolean = true,
)

interface MediaRepository {
    suspend fun list(query: MediaQuery): List<MediaItem>
    suspend fun get(id: String): MediaItem?
}

interface LocalMediaMutationAuthority {
    /** Delete only after explicit user authorization through the Android media boundary. */
    suspend fun delete(itemIds: Set<String>): MutationResult

    /** Move only after explicit user authorization; never infer destructive intent from sync. */
    suspend fun move(itemIds: Set<String>, destinationAlbumId: String): MutationResult
}

data class MutationResult(
    val completed: Set<String>,
    val rejected: Set<String>,
)
