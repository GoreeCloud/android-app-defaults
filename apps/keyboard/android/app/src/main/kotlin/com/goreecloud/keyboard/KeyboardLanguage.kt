package com.goreecloud.keyboard

import java.util.Locale

enum class KeyboardWritingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

enum class KeyboardLanguage(
    val primaryLanguageTag: String,
    val writingDirection: KeyboardWritingDirection,
    val supportsCaseShift: Boolean,
    val supportsLocalEnglishAssistance: Boolean,
    val spacebarLabel: String,
) {
    ENGLISH_US(
        primaryLanguageTag = "en-US",
        writingDirection = KeyboardWritingDirection.LEFT_TO_RIGHT,
        supportsCaseShift = true,
        supportsLocalEnglishAssistance = true,
        spacebarLabel = "English (US)",
    ),
    ARABIC(
        primaryLanguageTag = "ar",
        writingDirection = KeyboardWritingDirection.RIGHT_TO_LEFT,
        supportsCaseShift = false,
        supportsLocalEnglishAssistance = false,
        spacebarLabel = "العربية",
    );

    companion object {
        /**
         * Android's explicitly selected IME subtype is the sole language authority.
         * Device locale, editor text, application identity, and surrounding text are not inferred.
         */
        fun fromSubtypeLocale(localeValue: String?): KeyboardLanguage {
            val normalized = localeValue
                ?.trim()
                ?.replace('_', '-')
                ?.lowercase(Locale.ROOT)
                .orEmpty()
            return when (normalized.substringBefore('-')) {
                "ar" -> ARABIC
                else -> ENGLISH_US
            }
        }
    }
}
