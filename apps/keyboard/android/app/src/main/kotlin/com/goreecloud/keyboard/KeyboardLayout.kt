package com.goreecloud.keyboard

enum class KeyboardLayer {
    LETTERS,
    SYMBOLS,
    SYMBOLS_MORE,
    EMOJI,
}

enum class EmojiCategory(val label: String) {
    SMILEYS("☺"),
    PEOPLE("👤"),
    NATURE("🌿"),
    FOOD("🍽"),
    TRAVEL("✈"),
    SYMBOLS("★"),
}

object KeyboardLayout {
    private val letterRows = listOf(
        keys("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        keys("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        keys("z", "x", "c", "v", "b", "n", "m"),
    )
    private val symbolRows = listOf(
        keys("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        keys("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        keys("*", "\"", "'", ":", ";", "!", "?"),
    )
    private val moreSymbolRows = listOf(
        keys("[", "]", "{", "}", "<", ">", "=", "\\", "|"),
        keys("_", "^", "~", "`", "€", "£", "¥", "•", "…"),
        keys("±", "×", "÷", "§", "©", "®", "™", "—", "°"),
    )
    private val emojiRowsByCategory = mapOf(
        EmojiCategory.SMILEYS to listOf(
            keys("😀", "😃", "😄", "😁", "😆", "😅", "😂", "😊"),
            keys("🙂", "🥰", "😍", "🤩", "😎", "🤔", "😢", "😭"),
            keys("😡", "🥳", "😴", "🤗", "🙃", "😉", "😇", "🤭"),
        ),
        EmojiCategory.PEOPLE to listOf(
            keys("👍", "👍🏽", "👎", "👏", "🙌", "🙏🏾", "💪", "🤝"),
            keys("👋", "🫶", "👩‍💻", "👨‍💻", "🧑‍🚀", "👩‍🔬", "👨‍🔬", "🧑‍🍳"),
            keys("👨‍👩‍👧‍👦", "👩‍👩‍👦", "👨‍👨‍👧", "🧑‍🤝‍🧑", "🙋", "🙆", "🙅", "🤷"),
        ),
        EmojiCategory.NATURE to listOf(
            keys("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼"),
            keys("🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🦋"),
            keys("🌱", "🌿", "🍀", "🌵", "🌴", "🌻", "🌹", "🌊"),
        ),
        EmojiCategory.FOOD to listOf(
            keys("🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐"),
            keys("🍒", "🍑", "🥭", "🍍", "🥑", "🥕", "🌽", "🥦"),
            keys("🍞", "🥐", "🧀", "🍕", "🍔", "🍜", "🍣", "🍰"),
        ),
        EmojiCategory.TRAVEL to listOf(
            keys("🚗", "🚕", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒"),
            keys("✈️", "🚀", "🚁", "🚲", "🛴", "🚆", "🚇", "🚢"),
            keys("⛵", "🏖️", "🏕️", "🏔️", "🗺️", "🧳", "🎡", "🏟️"),
        ),
        EmojiCategory.SYMBOLS to listOf(
            keys("❤️", "⭐", "✅", "❌", "🔥", "🎉", "🚀", "💯"),
            keys("✨", "⚠️", "💡", "📌", "🌈", "🏳️‍🌈", "🇺🇸", "🇨🇦"),
            keys("☀️", "🌙", "☁️", "⚡", "❄️", "☕", "🎵", "🎁"),
        ),
    )

    fun characterRows(layer: KeyboardLayer): List<List<String>> = when (layer) {
        KeyboardLayer.LETTERS -> letterRows
        KeyboardLayer.SYMBOLS -> symbolRows
        KeyboardLayer.SYMBOLS_MORE -> moreSymbolRows
        KeyboardLayer.EMOJI -> emojiRows(EmojiCategory.SMILEYS)
    }

    fun emojiRows(category: EmojiCategory): List<List<String>> =
        emojiRowsByCategory.getValue(category)

    fun nextEmojiCategory(category: EmojiCategory): EmojiCategory {
        val categories = EmojiCategory.entries
        return categories[(category.ordinal + 1) % categories.size]
    }

    private fun keys(vararg values: String): List<String> = values.toList()
}