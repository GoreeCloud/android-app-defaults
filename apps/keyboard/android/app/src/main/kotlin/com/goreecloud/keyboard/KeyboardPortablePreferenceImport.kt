package com.goreecloud.keyboard

/**
 * Minimal write authority required to apply the currently approved portable Keyboard preference.
 *
 * Implementations must not infer or collect editor content, emoji recents, clipboard state, search
 * history, or other usage-derived data as a side effect of this write.
 */
fun interface EmojiCategoryPreferenceWriter {
    fun save(category: EmojiCategory)
}

/**
 * Explicit Development import seam for [KeyboardPortablePreferences].
 *
 * The encoded payload is fully decoded and validated before any write is attempted. Version 1 can
 * write exactly one low-sensitivity value: the explicitly selected emoji category.
 */
object KeyboardPortablePreferenceImport {
    sealed interface ApplyResult {
        data class Applied(val emojiCategory: EmojiCategory) : ApplyResult
        data class Invalid(val reason: String) : ApplyResult
    }

    fun apply(encoded: String, writer: EmojiCategoryPreferenceWriter): ApplyResult {
        return when (val decoded = KeyboardPortablePreferences.decode(encoded)) {
            is KeyboardPortablePreferences.DecodeResult.Invalid ->
                ApplyResult.Invalid(decoded.reason)

            is KeyboardPortablePreferences.DecodeResult.Success -> {
                writer.save(decoded.snapshot.emojiCategory)
                ApplyResult.Applied(decoded.snapshot.emojiCategory)
            }
        }
    }
}
