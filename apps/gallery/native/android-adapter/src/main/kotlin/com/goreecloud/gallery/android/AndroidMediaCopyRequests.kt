package com.goreecloud.gallery.android

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import java.io.IOException
import java.net.URI
import java.util.Collections

data class AndroidMediaCopySource(
    val contentUri: String,
    val displayName: String,
    val outputDisplayName: String,
    val mimeType: String,
    val capturedAtMillis: Long? = null,
)

data class AndroidMediaCopyResult(
    val copiedCount: Int,
    val failedCount: Int,
    val copiedContentUris: List<String>,
) {
    init {
        require(copiedCount >= 0)
        require(failedCount >= 0)
        require(copiedContentUris.size == copiedCount)
    }
}

/**
 * Copies exact Android-authorized MediaStore image/video items into new MediaStore rows.
 *
 * Copy never requests write authority over the source item and never updates its RELATIVE_PATH.
 * The source row is queried for its provider-owned concrete VOLUME_NAME so Gallery never attempts
 * insertion into MediaStore's synthetic external/internal aggregate volumes. Android 11+ copy
 * insertion also carries QUERY_ARG_RELATED_URI, which is Android's documented relationship hint
 * for copies that target an otherwise restricted MediaStore relative path.
 *
 * A destination row remains IS_PENDING until all source bytes have been written successfully.
 * Failed partial output rows are deleted best-effort before the operation continues.
 */
object AndroidMediaCopyRequests {
    const val MIN_SUPPORTED_API = Build.VERSION_CODES.R
    const val MAX_COPY_ITEMS = 100
    const val MAX_DISPLAY_NAME_CHARACTERS = 255

    fun isSupported(apiLevel: Int = Build.VERSION.SDK_INT): Boolean =
        apiLevel >= MIN_SUPPORTED_API

    fun execute(
        contentResolver: ContentResolver,
        sources: Collection<AndroidMediaCopySource>,
        destinationRelativePath: String,
    ): AndroidMediaCopyResult {
        check(Build.VERSION.SDK_INT >= MIN_SUPPORTED_API) {
            "MediaStore copy requires Android 11 or newer"
        }

        val normalizedSources = normalizeSources(sources)
        val destination = AndroidMediaMoveRequests.normalizeDestinationRelativePath(destinationRelativePath)
        val preparedSources = normalizedSources.map { source ->
            val sourceUri = Uri.parse(source.contentUri)
            PreparedCopySource(
                source = source,
                sourceUri = sourceUri,
                kind = sourceIdentity(source.contentUri).kind,
                volumeName = resolveConcreteVolumeName(contentResolver, sourceUri),
            )
        }
        require(preparedSources.map { it.volumeName }.distinct().size == 1) {
            "one Copy operation must remain on one concrete MediaStore volume"
        }

        val copiedUris = ArrayList<String>(preparedSources.size)
        var failedCount = 0

        preparedSources.forEach { prepared ->
            val source = prepared.source
            val sourceUri = prepared.sourceUri
            var outputUri: Uri? = null
            try {
                val collection = when (prepared.kind) {
                    MediaKind.IMAGE -> MediaStore.Images.Media.getContentUri(prepared.volumeName)
                    MediaKind.VIDEO -> MediaStore.Video.Media.getContentUri(prepared.volumeName)
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, source.outputDisplayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, source.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, destination)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    source.capturedAtMillis?.takeIf { it > 0L }?.let {
                        put(MediaStore.MediaColumns.DATE_TAKEN, it)
                    }
                }
                val extras = Bundle().apply {
                    putParcelable(MediaStore.QUERY_ARG_RELATED_URI, sourceUri)
                }

                outputUri = contentResolver.insert(collection, values, extras)
                    ?: throw IOException("MediaStore refused to create the copy output row")

                val input = contentResolver.openInputStream(sourceUri)
                    ?: throw IOException("MediaStore source stream is unavailable")
                val output = contentResolver.openOutputStream(outputUri, "w")
                    ?: throw IOException("MediaStore output stream is unavailable")
                input.use { inputStream ->
                    output.use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                }

                val publish = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                if (contentResolver.update(outputUri, publish, null, null) != 1) {
                    throw IOException("MediaStore copy output could not be published")
                }
                copiedUris += outputUri.toString()
                outputUri = null
            } catch (_: IllegalArgumentException) {
                failedCount += 1
            } catch (_: IllegalStateException) {
                failedCount += 1
            } catch (_: SecurityException) {
                failedCount += 1
            } catch (_: IOException) {
                failedCount += 1
            } catch (_: RuntimeException) {
                failedCount += 1
            } finally {
                outputUri?.let { uri ->
                    try {
                        contentResolver.delete(uri, null, null)
                    } catch (_: RuntimeException) {
                        // Best effort only: the row remains pending and was never intentionally published.
                    }
                }
            }
        }

        return AndroidMediaCopyResult(
            copiedCount = copiedUris.size,
            failedCount = failedCount,
            copiedContentUris = Collections.unmodifiableList(ArrayList(copiedUris)),
        )
    }

    internal fun normalizeSources(
        sources: Collection<AndroidMediaCopySource>,
    ): List<AndroidMediaCopySource> {
        require(sources.isNotEmpty()) { "at least one media item is required for Copy" }
        require(sources.size <= MAX_COPY_ITEMS) {
            "one Copy operation is limited to $MAX_COPY_ITEMS items"
        }

        val seen = HashSet<String>(sources.size)
        return sources.map { source ->
            val contentUri = AndroidMediaStoreItemUriPolicy.requireCanonicalItemUri(source.contentUri)
            require(seen.add(contentUri)) { "Copy source URIs must be unique" }

            val displayName = normalizeDisplayName(source.displayName)
            val outputDisplayName = normalizeDisplayName(source.outputDisplayName)
            val mimeType = source.mimeType.trim().lowercase()
            require(mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                "Copy supports only image/video media"
            }

            val identity = sourceIdentity(contentUri)
            require(
                (identity.kind == MediaKind.IMAGE && mimeType.startsWith("image/")) ||
                    (identity.kind == MediaKind.VIDEO && mimeType.startsWith("video/")),
            ) { "Copy MIME type must match the canonical MediaStore item collection" }

            AndroidMediaCopySource(
                contentUri = contentUri,
                displayName = displayName,
                outputDisplayName = outputDisplayName,
                mimeType = mimeType,
                capturedAtMillis = source.capturedAtMillis?.takeIf { it > 0L },
            )
        }
    }

    private fun normalizeDisplayName(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "media display name must not be blank" }
        require(value.length <= MAX_DISPLAY_NAME_CHARACTERS) {
            "media display name exceeds the supported size bound"
        }
        require('/' !in value && '\\' !in value && '\u0000' !in value) {
            "media display name contains unsupported path controls"
        }
        return value
    }

    private fun resolveConcreteVolumeName(
        contentResolver: ContentResolver,
        sourceUri: Uri,
    ): String {
        val volumeName = contentResolver.query(
            sourceUri,
            arrayOf(MediaStore.MediaColumns.VOLUME_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME)
                .takeIf { it >= 0 && !cursor.isNull(it) }
                ?.let(cursor::getString)
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        } ?: throw IOException("MediaStore source volume is unavailable")

        require(volumeName != MediaStore.VOLUME_EXTERNAL) {
            "Copy requires a concrete MediaStore external volume"
        }
        require(volumeName != MediaStore.VOLUME_INTERNAL) {
            "Copy cannot insert into MediaStore's synthetic internal volume"
        }
        return volumeName
    }

    private fun sourceIdentity(contentUri: String): MediaIdentity {
        val path = URI(contentUri).path.split('/').filter(String::isNotEmpty)
        require(path.size == 4 && path[2] == "media") {
            "Copy source must be a canonical MediaStore item"
        }
        val kind = when (path[1]) {
            "images" -> MediaKind.IMAGE
            "video" -> MediaKind.VIDEO
            else -> throw IllegalArgumentException("unsupported MediaStore collection")
        }
        return MediaIdentity(kind = kind)
    }

    private data class MediaIdentity(
        val kind: MediaKind,
    )

    private data class PreparedCopySource(
        val source: AndroidMediaCopySource,
        val sourceUri: Uri,
        val kind: MediaKind,
        val volumeName: String,
    )

    private enum class MediaKind {
        IMAGE,
        VIDEO,
    }
}
