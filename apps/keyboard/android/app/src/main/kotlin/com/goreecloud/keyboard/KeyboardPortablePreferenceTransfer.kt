package com.goreecloud.keyboard

/**
 * File-transfer policy for the current one-field portable Keyboard preference format.
 *
 * This helper never reads editor state, emoji recents, clipboard contents, search history,
 * telemetry, Identity data, or credentials. It only bridges explicit user-selected file bytes to
 * the already-reviewed category-only export/import seams.
 */
object KeyboardPortablePreferenceTransfer {
    const val MIME_TYPE = "text/plain"
    const val EXPORT_FILE_NAME = "goreecloud-keyboard-preferences.txt"
    const val MAX_IMPORT_BYTES = 4096

    @ConsistentCopyVisibility
    data class ExportPreview internal constructor(val emojiCategory: EmojiCategory)

    @ConsistentCopyVisibility
    data class ImportPreview internal constructor(val emojiCategory: EmojiCategory)

    sealed interface PreviewResult {
        data class Ready(val preview: ImportPreview) : PreviewResult
        data class Rejected(val reason: String) : PreviewResult
    }

    fun previewExport(reader: EmojiCategoryPreferenceReader): ExportPreview =
        ExportPreview(reader.loadEmojiCategory())

    fun exportPreviewBytes(preview: ExportPreview): ByteArray =
        KeyboardPortablePreferences.encode(
            KeyboardPortablePreferences.Snapshot(preview.emojiCategory)
        ).toByteArray(Charsets.UTF_8)

    /**
     * Persist only the already-reviewed category while Android temporarily owns the export flow.
     *
     * The saved value deliberately excludes editor state, recents, clipboard contents, search
     * history, and every other Keyboard-owned state category. Unknown or altered values fail
     * closed instead of being coerced into another export category.
     */
    fun exportPreviewStateValue(preview: ExportPreview): String = preview.emojiCategory.name

    fun restoreExportPreviewState(value: String?): ExportPreview? {
        if (value.isNullOrBlank()) return null
        val category = EmojiCategory.entries.firstOrNull { it.name == value } ?: return null
        return ExportPreview(category)
    }

    fun exportBytes(reader: EmojiCategoryPreferenceReader): ByteArray =
        exportPreviewBytes(previewExport(reader))

    /**
     * Validate and decode an explicitly selected file without invoking any preference writer.
     */
    fun previewImportBytes(bytes: ByteArray): PreviewResult {
        if (bytes.size > MAX_IMPORT_BYTES) {
            return PreviewResult.Rejected("portable preference file exceeds the bounded size limit")
        }

        val encoded = bytes.toString(Charsets.UTF_8)
        if (!encoded.toByteArray(Charsets.UTF_8).contentEquals(bytes)) {
            return PreviewResult.Rejected("portable preference file must be canonical UTF-8")
        }

        return when (val decoded = KeyboardPortablePreferences.decode(encoded)) {
            is KeyboardPortablePreferences.DecodeResult.Invalid ->
                PreviewResult.Rejected(decoded.reason)

            is KeyboardPortablePreferences.DecodeResult.Success ->
                PreviewResult.Ready(ImportPreview(decoded.snapshot.emojiCategory))
        }
    }

    /**
     * Apply only a previously validated typed preview after an explicit user confirmation.
     */
    fun applyPreview(
        preview: ImportPreview,
        writer: EmojiCategoryPreferenceWriter,
    ): EmojiCategory {
        writer.save(preview.emojiCategory)
        return preview.emojiCategory
    }
}
