package com.goreecloud.keyboard

/**
 * Minimal read authority required to export the currently approved portable Keyboard preference.
 *
 * The reader exposes exactly one explicit low-sensitivity preference. It has no surface for editor
 * content, emoji recents, clipboard state, search history, learned input, telemetry, or credentials.
 */
fun interface EmojiCategoryPreferenceReader {
    fun loadEmojiCategory(): EmojiCategory
}

/**
 * Explicit Development export seam for [KeyboardPortablePreferences].
 *
 * Version 1 reads exactly the last explicitly selected emoji category and serializes that value
 * through the existing strict portable format.
 */
object KeyboardPortablePreferenceExport {
    fun create(reader: EmojiCategoryPreferenceReader): String =
        KeyboardPortablePreferences.encode(
            KeyboardPortablePreferences.Snapshot(
                emojiCategory = reader.loadEmojiCategory(),
            ),
        )
}
