package com.goreecloud.keyboard

/**
 * Bounded recency model for emoji committed from the keyboard surface.
 *
 * Exact String values are retained so multi-code-point emoji remain intact. The
 * model itself has no persistence or network authority; callers may restore and
 * save the bounded list through the private local store.
 */
class EmojiRecents(
    private val limit: Int = DEFAULT_LIMIT,
    private val rowWidth: Int = DEFAULT_ROW_WIDTH,
    initialValues: List<String> = emptyList(),
) {
    private val entries = mutableListOf<String>()

    init {
        require(limit > 0) { "emoji recent limit must be positive" }
        require(rowWidth > 0) { "emoji recent row width must be positive" }
        restore(initialValues)
    }

    fun record(value: String) {
        if (value.isBlank()) return
        entries.remove(value)
        entries.add(0, value)
        while (entries.size > limit) entries.removeAt(entries.lastIndex)
    }

    fun restore(values: List<String>) {
        entries.clear()
        values.asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .take(limit)
            .forEach(entries::add)
    }

    fun clear() {
        entries.clear()
    }

    fun values(): List<String> = entries.toList()

    fun rows(rowCount: Int = DEFAULT_ROW_COUNT): List<List<String>> {
        require(rowCount > 0) { "emoji recent row count must be positive" }
        val visible = entries.take(rowWidth * rowCount).chunked(rowWidth).toMutableList()
        while (visible.size < rowCount) visible.add(emptyList())
        return visible
    }

    companion object {
        const val DEFAULT_LIMIT = 24
        const val DEFAULT_ROW_WIDTH = 8
        const val DEFAULT_ROW_COUNT = 3
    }
}
