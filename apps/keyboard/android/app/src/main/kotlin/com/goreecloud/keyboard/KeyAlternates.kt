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
    private val englishAlternatives = mapOf(
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

    private val arabicAlternatives = mapOf(
        "ا" to listOf("أ", "إ", "آ", "ٱ"),
        "و" to listOf("ؤ"),
        "ي" to listOf("ئ", "ى"),
        "ه" to listOf("ة"),
        "ء" to listOf("َ", "ُ", "ِ", "ْ", "ّ", "ً", "ٌ", "ٍ"),
        "،" to listOf("؛"),
        "." to listOf("؟", "؛"),
    )

    fun forKey(
        label: String,
        language: KeyboardLanguage = KeyboardLayout.currentLanguage(),
    ): List<String> {
        if (label.isEmpty()) return emptyList()
        if (language == KeyboardLanguage.ARABIC) {
            return arabicAlternatives[label].orEmpty()
        }
        val normalized = label.lowercase(Locale.ROOT)
        val baseAlternates = englishAlternatives[normalized] ?: return emptyList()
        val shouldUppercase = label != normalized && label == label.uppercase(Locale.ROOT)
        return if (shouldUppercase) {
            baseAlternates.map { it.uppercase(Locale.ROOT) }
        } else {
            baseAlternates
        }
    }
}
