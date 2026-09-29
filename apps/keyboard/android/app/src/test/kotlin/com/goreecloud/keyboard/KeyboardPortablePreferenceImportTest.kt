package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPortablePreferenceImportTest {
    @Test
    fun validSnapshotWritesExactlyTheDecodedCategory() {
        var written: EmojiCategory? = null
        var writes = 0
        val writer = EmojiCategoryPreferenceWriter { category ->
            writes += 1
            written = category
        }
        val encoded = KeyboardPortablePreferences.encode(
            KeyboardPortablePreferences.Snapshot(EmojiCategory.TRAVEL)
        )

        val result = KeyboardPortablePreferenceImport.apply(encoded, writer)

        assertTrue(result is KeyboardPortablePreferenceImport.ApplyResult.Applied)
        assertEquals(EmojiCategory.TRAVEL, written)
        assertEquals(1, writes)
    }

    @Test
    fun invalidSnapshotNeverInvokesWriter() {
        var written: EmojiCategory? = null
        var writes = 0
        val writer = EmojiCategoryPreferenceWriter { category ->
            writes += 1
            written = category
        }
        val encoded = KeyboardPortablePreferences.encode(
            KeyboardPortablePreferences.Snapshot(EmojiCategory.SMILEYS)
        ).replace("emoji_category=SMILEYS", "emoji_category=OBJECTS")

        val result = KeyboardPortablePreferenceImport.apply(encoded, writer)

        assertTrue(result is KeyboardPortablePreferenceImport.ApplyResult.Invalid)
        assertNull(written)
        assertEquals(0, writes)
    }

    @Test
    fun versionOneCannotWriteUsageDerivedOrEditorState() {
        val methods = EmojiCategoryPreferenceWriter::class.java.methods
            .filter { it.declaringClass == EmojiCategoryPreferenceWriter::class.java }
            .map { method -> method.name }
        assertEquals(listOf("save"), methods)

        val save = EmojiCategoryPreferenceWriter::class.java.getMethod("save", EmojiCategory::class.java)
        assertEquals(listOf(EmojiCategory::class.java), save.parameterTypes.toList())
    }
}
