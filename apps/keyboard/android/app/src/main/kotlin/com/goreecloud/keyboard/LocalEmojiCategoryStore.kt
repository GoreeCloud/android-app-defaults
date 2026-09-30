package com.goreecloud.keyboard

import android.content.Context

/**
 * Private on-device persistence for the last explicitly selected emoji category.
 *
 * The value is a bounded enum name only. It is not synchronized, logged,
 * transmitted, or used as learned-language/personality data.
 */
class LocalEmojiCategoryStore(context: Context) :
    EmojiCategoryPreferenceWriter,
    EmojiCategoryPreferenceReader {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): EmojiCategory = EmojiCategoryPreference.decode(preferences.getString(KEY_CATEGORY, null))

    override fun loadEmojiCategory(): EmojiCategory = load()

    override fun save(category: EmojiCategory) {
        preferences.edit()
            .putString(KEY_CATEGORY, EmojiCategoryPreference.encode(category))
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "goreecloud_keyboard_local_preferences"
        private const val KEY_CATEGORY = "emoji_category_v1"
    }
}
