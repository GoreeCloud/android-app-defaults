package com.goreecloud.keyboard

import android.content.Context

/**
 * Private on-device persistence for the bounded emoji recents list.
 *
 * This store uses Android MODE_PRIVATE application preferences only. It does not
 * synchronize, log, transmit, or expose the values to another GoreeCloud service.
 */
class LocalEmojiRecentsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): List<String> = EmojiRecentsCodec.decode(preferences.getString(KEY_RECENTS, null))

    fun save(values: List<String>) {
        val encoded = EmojiRecentsCodec.encode(values)
        preferences.edit().apply {
            if (encoded.isEmpty()) remove(KEY_RECENTS) else putString(KEY_RECENTS, encoded)
        }.apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "goreecloud_keyboard_local_preferences"
        private const val KEY_RECENTS = "emoji_recents_v1"
    }
}
