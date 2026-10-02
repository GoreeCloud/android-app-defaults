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
    @Volatile
    private var activeLanguage = KeyboardLanguage.ENGLISH_US

    private val englishNumberRow = keys("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val arabicNumberRow = keys("١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "٠")

    private val englishLetterRows = listOf(
        keys("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        keys("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        keys("z", "x", "c", "v", "b", "n", "m"),
    )
    private val arabicLetterRows = listOf(
        keys("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"),
        keys("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"),
        keys("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ"),
    )
    private val englishSymbolRows = listOf(
        englishNumberRow,
        keys("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        keys("*", "\"", "'", ":", ";", "!", "?"),
    )
    private val arabicSymbolRows = listOf(
        arabicNumberRow,
        keys("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        keys("*", "\"", "'", ":", "؛", "!", "؟"),
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

    fun activateLanguage(language: KeyboardLanguage) {
        activeLanguage = language
    }

    fun currentLanguage(): KeyboardLanguage = activeLanguage

    fun numberRow(language: KeyboardLanguage = activeLanguage): List<String> =
        when (language) {
            KeyboardLanguage.ENGLISH_US -> englishNumberRow
            KeyboardLanguage.ARABIC -> arabicNumberRow
        }

    fun primaryComma(language: KeyboardLanguage = activeLanguage): String =
        when (language) {
            KeyboardLanguage.ENGLISH_US -> ","
            KeyboardLanguage.ARABIC -> "،"
        }

    fun characterRows(
        layer: KeyboardLayer,
        language: KeyboardLanguage = activeLanguage,
    ): List<List<String>> = when (layer) {
        KeyboardLayer.LETTERS -> when (language) {
            KeyboardLanguage.ENGLISH_US -> englishLetterRows
            KeyboardLanguage.ARABIC -> arabicLetterRows
        }
        KeyboardLayer.SYMBOLS -> when (language) {
            KeyboardLanguage.ENGLISH_US -> englishSymbolRows
            KeyboardLanguage.ARABIC -> arabicSymbolRows
        }
        KeyboardLayer.SYMBOLS_MORE -> moreSymbolRows
        KeyboardLayer.EMOJI -> emojiRows(EmojiCategory.SMILEYS)
    }

    fun directLetterPunctuation(
        language: KeyboardLanguage = activeLanguage,
    ): Pair<String, String> = when (language) {
        KeyboardLanguage.ENGLISH_US -> "," to "."
        KeyboardLanguage.ARABIC -> "،" to "."
    }

    fun emojiRows(category: EmojiCategory): List<List<String>> =
        emojiRowsByCategory.getValue(category)

    fun nextEmojiCategory(category: EmojiCategory): EmojiCategory {
        val categories = EmojiCategory.entries
        return categories[(category.ordinal + 1) % categories.size]
    }

    private fun keys(vararg values: String): List<String> = values.toList()
}