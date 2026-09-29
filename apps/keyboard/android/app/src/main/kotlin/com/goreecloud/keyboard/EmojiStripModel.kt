package com.goreecloud.keyboard

data class EmojiStripEntry(
    val visibleLabel: String,
    val accessibilityLabel: String,
    val category: EmojiCategory? = null,
    val recent: Boolean = false,
    val clearRecents: Boolean = false,
    val search: Boolean = false,
)

object EmojiStripModel {
    fun entries(hasRecents: Boolean): List<EmojiStripEntry> = buildList {
        add(
            EmojiStripEntry(
                visibleLabel = "⌕",
                accessibilityLabel = "Search emoji",
                search = true,
            ),
        )
        if (hasRecents) {
            add(
                EmojiStripEntry(
                    visibleLabel = "◷",
                    accessibilityLabel = "Recent emoji",
                    recent = true,
                ),
            )
            add(
                EmojiStripEntry(
                    visibleLabel = "×",
                    accessibilityLabel = "Clear recent emoji",
                    clearRecents = true,
                ),
            )
        }
        EmojiCategory.entries.forEach { category ->
            add(
                EmojiStripEntry(
                    visibleLabel = category.label,
                    accessibilityLabel = "${category.displayName()} emoji",
                    category = category,
                ),
            )
        }
    }

    private fun EmojiCategory.displayName(): String =
        name.lowercase().replaceFirstChar { it.uppercase() }
}
