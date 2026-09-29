package com.goreecloud.keyboard

import java.util.Locale

/**
 * Device-local long-press alternates for the primary keyboard surface.
 *
 * This catalog performs no network lookup, learning, personalization, clipboard read,
 * text-context inspection, persistence, or sensitive-field inference. Rendering and
 * gesture ownership remain separate UI work.
 */
object KeyAlternates {
    private val alternatives = mapOf(
        "a" to listOf("á", "à", "â", "ä", "ã", "å", "æ"),
        "c" to listOf("ç"),
        "e" to listOf("é", "è", "ê", "ë"),
        "i" to listOf("í", "ì", "î", "ï"),
        "n" to listOf("ñ"),
        "o" to listOf("ó", "ò", "ô", "ö", "õ", "ø", "œ"),
        "u" to listOf("ú", "ù", "û", "ü"),
        "y" to listOf("ý", "ÿ"),
        "." to listOf("…"),
        "-" to listOf("–", "—"),
        "'" to listOf("’", "‘"),
        "\"" to listOf("”", "“"),
        "?" to listOf("¿"),
        "!" to listOf("¡"),
    )

    fun forKey(label: String): List<String> {
        if (label.isEmpty()) return emptyList()
        val normalized = label.lowercase(Locale.ROOT)
        val baseAlternates = alternatives[normalized] ?: return emptyList()
        val shouldUppercase = label != normalized && label == label.uppercase(Locale.ROOT)
        return if (shouldUppercase) {
            baseAlternates.map { it.uppercase(Locale.ROOT) }
        } else {
            baseAlternates
        }
    }
}
