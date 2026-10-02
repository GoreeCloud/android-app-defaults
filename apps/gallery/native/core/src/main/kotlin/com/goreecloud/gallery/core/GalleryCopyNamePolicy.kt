package com.goreecloud.gallery.core

object GalleryCopyNamePolicy {
    fun nextAvailable(
        sourceDisplayName: String,
        occupiedDisplayNames: Set<String>,
    ): String {
        val source = sourceDisplayName.trim()
        require(source.isNotEmpty()) { "source display name is required" }
        require('/' !in source && '\\' !in source && '\u0000' !in source) {
            "source display name contains unsupported path controls"
        }

        val dot = source.lastIndexOf('.')
        val hasExtension = dot > 0 && dot < source.lastIndex
        val stem = if (hasExtension) source.substring(0, dot) else source
        val extension = if (hasExtension) source.substring(dot) else ""
        val occupied = occupiedDisplayNames.map { it.lowercase() }.toHashSet()

        for (copyIndex in 1..999) {
            val suffix = if (copyIndex == 1) " (copy)" else " (copy $copyIndex)"
            val candidate = "$stem$suffix$extension"
            if (candidate.lowercase() !in occupied) return candidate
        }
        throw IllegalStateException("no bounded copy display name is available")
    }
}
