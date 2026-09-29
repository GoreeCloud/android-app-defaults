package com.goreecloud.keyboard

import java.util.Locale

data class EmojiSearchResult(
    val emoji: String,
    val category: EmojiCategory,
)

object OfflineEmojiSearch {
    const val MAX_RESULTS = 24

    private val keywords = mapOf(
        "😀" to "grin happy smile face",
        "😂" to "laugh laughing tears joy funny",
        "😊" to "smile happy blush face",
        "😍" to "love heart eyes face",
        "😎" to "cool sunglasses face",
        "😢" to "sad cry tear face",
        "😭" to "cry crying tears sad face",
        "😡" to "angry mad face",
        "👍" to "thumb up yes approve like",
        "👍🏽" to "thumb up yes approve like",
        "👎" to "thumb down no dislike",
        "👏" to "clap applause hands",
        "🙏🏾" to "pray thanks please hands",
        "👋" to "wave hello goodbye hand",
        "🫶" to "heart hands love",
        "👩‍💻" to "woman technologist computer developer",
        "👨‍💻" to "man technologist computer developer",
        "🐶" to "dog puppy pet animal",
        "🐱" to "cat kitten pet animal",
        "🦋" to "butterfly nature insect",
        "🌿" to "leaf herb plant nature",
        "🌻" to "sunflower flower nature",
        "🌊" to "wave ocean water sea",
        "🍎" to "apple fruit food",
        "🍕" to "pizza food",
        "🍔" to "burger hamburger food",
        "🍜" to "noodles bowl food",
        "🍣" to "sushi food",
        "🍰" to "cake dessert food",
        "🚗" to "car auto automobile vehicle travel",
        "🚕" to "taxi cab car travel",
        "🚌" to "bus transit travel",
        "✈️" to "plane airplane flight travel",
        "🚀" to "rocket space launch travel",
        "🚲" to "bike bicycle cycling travel",
        "🚆" to "train rail travel",
        "🚇" to "metro subway transit travel",
        "🚢" to "ship boat cruise travel",
        "🏖️" to "beach vacation travel",
        "🏕️" to "camp camping tent travel",
        "🏔️" to "mountain travel nature",
        "🗺️" to "map world travel navigation",
        "🧳" to "luggage suitcase travel trip",
        "❤️" to "heart love symbol",
        "⭐" to "star favorite symbol",
        "✅" to "check yes done success symbol",
        "❌" to "cross no cancel error symbol",
        "🔥" to "fire hot flame",
        "🎉" to "party celebrate celebration",
        "💯" to "hundred perfect score",
        "⚠️" to "warning alert caution",
        "💡" to "idea light bulb",
        "📌" to "pin pushpin",
        "☕" to "coffee drink cup",
        "🎵" to "music note song",
        "🎁" to "gift present",
    )

    fun search(query: String, limit: Int = MAX_RESULTS): List<EmojiSearchResult> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return emptyList()
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val boundedLimit = limit.coerceIn(0, MAX_RESULTS)
        if (boundedLimit == 0) return emptyList()

        val results = mutableListOf<EmojiSearchResult>()
        val seen = mutableSetOf<String>()
        for (category in EmojiCategory.entries) {
            val categoryName = category.name.lowercase(Locale.ROOT)
            for (emoji in KeyboardLayout.emojiRows(category).flatten()) {
                if (!seen.add(emoji)) continue
                val haystack = buildString {
                    append(categoryName)
                    append(' ')
                    append(keywords[emoji].orEmpty())
                    append(' ')
                    append(emoji)
                }
                if (tokens.all { token -> haystack.contains(token) }) {
                    results += EmojiSearchResult(emoji = emoji, category = category)
                    if (results.size == boundedLimit) return results
                }
            }
        }
        return results
    }
}
