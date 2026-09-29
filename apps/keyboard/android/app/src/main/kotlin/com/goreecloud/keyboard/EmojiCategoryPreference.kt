package com.goreecloud.keyboard

object EmojiCategoryPreference {
    fun encode(category: EmojiCategory): String = category.name

    fun decode(value: String?): EmojiCategory =
        value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { stored -> EmojiCategory.entries.firstOrNull { it.name == stored } }
            ?: EmojiCategory.SMILEYS
}
