package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPortablePreferenceTransferTest {
    @Test
    fun exportBytesContainOnlyTheApprovedCategorySnapshot() {
        val bytes = KeyboardPortablePreferenceTransfer.exportBytes(
            EmojiCategoryPreferenceReader { EmojiCategory.NATURE }
        )
        val encoded = bytes.toString(Charsets.UTF_8)

        assertTrue(encoded.contains("emoji_category=NATURE"))
        assertFalse(encoded.contains("emoji_recents"))
        assertFalse(encoded.contains("typed_text"))
        assertFalse(encoded.contains("search_query"))
        assertFalse(encoded.contains("clipboard"))
    }

    @Test
    fun exportPreviewFreezesReviewedCategoryBeforeDestinationSelection() {
        var currentCategory = EmojiCategory.NATURE
        val preview = KeyboardPortablePreferenceTransfer.previewExport(
            EmojiCategoryPreferenceReader { currentCategory }
        )

        currentCategory = EmojiCategory.FOOD
        val encoded = KeyboardPortablePreferenceTransfer.exportPreviewBytes(preview)
            .toString(Charsets.UTF_8)

        assertTrue(encoded.contains("emoji_category=NATURE"))
        assertFalse(encoded.contains("emoji_category=FOOD"))
    }

    @Test
    fun reviewedExportStateRoundTripsOnlyTheFrozenCategory() {
        val preview = KeyboardPortablePreferenceTransfer.previewExport(
            EmojiCategoryPreferenceReader { EmojiCategory.TRAVEL }
        )

        val persisted = KeyboardPortablePreferenceTransfer.exportPreviewStateValue(preview)
        val restored = KeyboardPortablePreferenceTransfer.restoreExportPreviewState(persisted)

        assertEquals("TRAVEL", persisted)
        assertEquals(EmojiCategory.TRAVEL, restored?.emojiCategory)
    }

    @Test
    fun alteredReviewedExportStateFailsClosed() {
        assertNull(KeyboardPortablePreferenceTransfer.restoreExportPreviewState(null))
        assertNull(KeyboardPortablePreferenceTransfer.restoreExportPreviewState(""))
        assertNull(KeyboardPortablePreferenceTransfer.restoreExportPreviewState("not-a-category"))
    }

    @Test
    fun exportPreviewShapeContainsOnlyOneCategoryField() {
        val fields = KeyboardPortablePreferenceTransfer.ExportPreview::class.java.declaredFields
            .filterNot { it.isSynthetic }

        assertEquals(1, fields.size)
        assertEquals(EmojiCategory::class.java, fields.single().type)
    }

    @Test
    fun validBytesPreviewWithoutAnyPreferenceWrite() {
        val bytes = KeyboardPortablePreferenceTransfer.exportBytes(
            EmojiCategoryPreferenceReader { EmojiCategory.FOOD }
        )

        val result = KeyboardPortablePreferenceTransfer.previewImportBytes(bytes)

        assertTrue(result is KeyboardPortablePreferenceTransfer.PreviewResult.Ready)
        val ready = result as KeyboardPortablePreferenceTransfer.PreviewResult.Ready
        assertEquals(EmojiCategory.FOOD, ready.preview.emojiCategory)
    }

    @Test
    fun confirmedPreviewAppliesExactlyOneCategoryWrite() {
        val writes = mutableListOf<EmojiCategory>()
        val bytes = KeyboardPortablePreferenceTransfer.exportBytes(
            EmojiCategoryPreferenceReader { EmojiCategory.FOOD }
        )
        val ready = KeyboardPortablePreferenceTransfer.previewImportBytes(bytes)
            as KeyboardPortablePreferenceTransfer.PreviewResult.Ready

        val applied = KeyboardPortablePreferenceTransfer.applyPreview(
            ready.preview,
            EmojiCategoryPreferenceWriter { writes += it },
        )

        assertEquals(EmojiCategory.FOOD, applied)
        assertEquals(listOf(EmojiCategory.FOOD), writes)
    }

    @Test
    fun oversizedFileIsRejectedDuringPreview() {
        val result = KeyboardPortablePreferenceTransfer.previewImportBytes(
            ByteArray(KeyboardPortablePreferenceTransfer.MAX_IMPORT_BYTES + 1) { 'x'.code.toByte() }
        )

        assertTrue(result is KeyboardPortablePreferenceTransfer.PreviewResult.Rejected)
    }

    @Test
    fun malformedUtf8IsRejectedDuringPreview() {
        val result = KeyboardPortablePreferenceTransfer.previewImportBytes(
            byteArrayOf(0xC3.toByte(), 0x28)
        )

        assertTrue(result is KeyboardPortablePreferenceTransfer.PreviewResult.Rejected)
    }

    @Test
    fun tamperedPortablePayloadIsRejectedDuringPreview() {
        val bytes = KeyboardPortablePreferenceTransfer.exportBytes(
            EmojiCategoryPreferenceReader { EmojiCategory.SYMBOLS }
        )
        val tampered = bytes.toString(Charsets.UTF_8)
            .replace("emoji_category=SYMBOLS", "emoji_category=SMILEYS")
            .toByteArray(Charsets.UTF_8)

        val result = KeyboardPortablePreferenceTransfer.previewImportBytes(tampered)

        assertTrue(result is KeyboardPortablePreferenceTransfer.PreviewResult.Rejected)
    }
}
