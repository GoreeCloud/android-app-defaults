package com.goreecloud.gallery.core

object GalleryCopyNamePolicy {
    const val MAX_DISPLAY_NAME_CHARACTERS = 255
    private const val MAX_PRESERVED_EXTENSION_CHARACTERS = 32

    fun nextAvailable(
        sourceDisplayName: String,
        occupiedDisplayNames: Set<String>,
    ): String {
        val source = sourceDisplayName.trim()
        require(source.isNotEmpty()) { "source display name is required" }
        require(source.length <= MAX_DISPLAY_NAME_CHARACTERS) {
            "source display name exceeds the supported size bound"
        }
        require('/' !in source && '\\' !in source && '\u0000' !in source) {
            "source display name contains unsupported path controls"
        }

        val dot = source.lastIndexOf('.')
        val rawExtension = if (dot > 0 && dot < source.lastIndex) source.substring(dot) else ""
        val extension = rawExtension.takeIf { it.length <= MAX_PRESERVED_EXTENSION_CHARACTERS }.orEmpty()
        val rawStem = if (extension.isNotEmpty()) source.substring(0, dot) else source
        val occupied = occupiedDisplayNames.map { it.lowercase() }.toHashSet()

        for (copyIndex in 1..999) {
            val suffix = if (copyIndex == 1) " (copy)" else " (copy $copyIndex)"
            val stemBudget = MAX_DISPLAY_NAME_CHARACTERS - suffix.length - extension.length
            require(stemBudget > 0) { "copy display name cannot fit within the supported size bound" }
            val stem = rawStem.take(stemBudget).trimEnd().ifBlank { "Media".take(stemBudget) }
            val candidate = "$stem$suffix$extension"
            if (candidate.lowercase() !in occupied) return candidate
        }
        throw IllegalStateException("no bounded copy display name is available")
    }
}
