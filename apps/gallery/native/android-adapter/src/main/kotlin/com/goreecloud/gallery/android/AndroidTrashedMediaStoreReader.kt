package com.goreecloud.gallery.android

import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.goreecloud.gallery.core.MediaItem
import com.goreecloud.gallery.core.MediaStoreProjection
import com.goreecloud.gallery.core.MediaStoreRow

/**
 * Reads only Android MediaStore items whose authoritative IS_TRASHED state is set.
 *
 * This reader deliberately keeps Android MediaStore as the Trash authority. It does not create an
 * app-private duplicate trash database, broaden filesystem access, or infer deleted state from a
 * Gallery-owned list. Android 11+ is required because QUERY_ARG_MATCH_TRASHED was added in API 30.
 * The provider request itself is bounded to the same validated row ceiling enforced while consuming
 * the returned cursor, so a cooperative MediaStore provider need not materialize an unnecessarily
 * large Trash result merely for Gallery to discard rows past its Development presentation bound.
 */
class AndroidTrashedMediaStoreReader(
    private val contentResolver: ContentResolver,
) {
    fun readLatest(maxRows: Int = DEFAULT_MAX_ROWS): AndroidMediaStoreReadResult {
        check(isSupported()) {
            "Android MediaStore Trash browsing requires Android 11 or newer"
        }
        require(maxRows in 1..MAX_ROWS) { "maxRows must be between 1 and $MAX_ROWS" }

        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val volumeName = MediaStore.getVolumeName(collection)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val sortOrder = listOf(
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
            "${MediaStore.MediaColumns._ID} DESC",
        ).joinToString(", ")
        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            putInt(ContentResolver.QUERY_ARG_LIMIT, maxRows)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        val cursor = contentResolver.query(
            collection,
            AndroidMediaStoreProjection.columns.toTypedArray(),
            queryArgs,
            null,
        ) ?: throw MediaStoreQueryUnavailableException("MediaStore Trash query returned no cursor")

        cursor.use {
            val indices = ColumnIndices.from(it)
            val items = ArrayList<MediaItem>(minOf(maxRows, 64))
            var rejected = 0
            var inspected = 0
            // Keep the consumer-side ceiling even when an OEM/provider ignores QUERY_ARG_LIMIT.
            while (inspected < maxRows && it.moveToNext()) {
                inspected += 1
                try {
                    val row = indices.readRow(it, collection.toString())
                    val itemCollectionUri = AndroidMediaStoreItemUris.collectionUriForMimeType(
                        volumeName = volumeName,
                        mimeType = row.mimeType,
                    )
                    items += row.copy(collectionUri = itemCollectionUri).toMediaItem()
                } catch (_: IllegalArgumentException) {
                    rejected += 1
                }
            }
            return AndroidMediaStoreReadResult(
                items = items.toList(),
                rejectedRowCount = rejected,
            )
        }
    }

    private data class ColumnIndices(
        val id: Int,
        val displayName: Int,
        val mimeType: Int,
        val dateTaken: Int,
        val dateModified: Int,
        val width: Int,
        val height: Int,
        val duration: Int,
        val size: Int,
        val bucketId: Int,
        val bucketDisplayName: Int,
    ) {
        fun readRow(cursor: Cursor, collectionUri: String): MediaStoreRow = MediaStoreRow(
            collectionUri = collectionUri,
            id = cursor.getRequiredLong(id, MediaStoreProjection.ID),
            displayName = cursor.getRequiredString(displayName, MediaStoreProjection.DISPLAY_NAME),
            mimeType = cursor.getRequiredString(mimeType, MediaStoreProjection.MIME_TYPE),
            dateTakenEpochMillis = cursor.getNullableLong(dateTaken),
            dateModifiedEpochSeconds = cursor.getRequiredLong(dateModified, MediaStoreProjection.DATE_MODIFIED),
            width = cursor.getPositiveNullableInt(width),
            height = cursor.getPositiveNullableInt(height),
            durationMillis = cursor.getNullableLong(duration),
            sizeBytes = cursor.getRequiredLong(size, MediaStoreProjection.SIZE),
            bucketId = cursor.getNullableString(bucketId),
            bucketDisplayName = cursor.getNullableString(bucketDisplayName),
        )

        companion object {
            fun from(cursor: Cursor): ColumnIndices = ColumnIndices(
                id = cursor.getColumnIndexOrThrow(MediaStoreProjection.ID),
                displayName = cursor.getColumnIndexOrThrow(MediaStoreProjection.DISPLAY_NAME),
                mimeType = cursor.getColumnIndexOrThrow(MediaStoreProjection.MIME_TYPE),
                dateTaken = cursor.getColumnIndexOrThrow(MediaStoreProjection.DATE_TAKEN),
                dateModified = cursor.getColumnIndexOrThrow(MediaStoreProjection.DATE_MODIFIED),
                width = cursor.getColumnIndexOrThrow(MediaStoreProjection.WIDTH),
                height = cursor.getColumnIndexOrThrow(MediaStoreProjection.HEIGHT),
                duration = cursor.getColumnIndexOrThrow(MediaStoreProjection.DURATION),
                size = cursor.getColumnIndexOrThrow(MediaStoreProjection.SIZE),
                bucketId = cursor.getColumnIndexOrThrow(MediaStoreProjection.BUCKET_ID),
                bucketDisplayName = cursor.getColumnIndexOrThrow(MediaStoreProjection.BUCKET_DISPLAY_NAME),
            )
        }
    }

    companion object {
        const val MIN_SUPPORTED_API = Build.VERSION_CODES.R
        const val DEFAULT_MAX_ROWS = 250
        const val MAX_ROWS = 500

        fun isSupported(apiLevel: Int = Build.VERSION.SDK_INT): Boolean =
            apiLevel >= MIN_SUPPORTED_API
    }
}

private fun Cursor.getRequiredLong(index: Int, column: String): Long {
    require(!isNull(index)) { "$column is required" }
    return getLong(index)
}

private fun Cursor.getRequiredString(index: Int, column: String): String {
    require(!isNull(index)) { "$column is required" }
    return getString(index)
}

private fun Cursor.getNullableLong(index: Int): Long? = if (isNull(index)) null else getLong(index)

private fun Cursor.getNullableString(index: Int): String? = if (isNull(index)) null else getString(index)

private fun Cursor.getPositiveNullableInt(index: Int): Int? {
    if (isNull(index)) return null
    return getInt(index).takeIf { it > 0 }
}
