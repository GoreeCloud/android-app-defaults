package com.goreecloud.keyboard

import java.text.Normalizer

/**
 * Bounded first-party Arabic prefix completion for the explicitly selected Arabic IME subtype.
 *
 * This is intentionally not a correction, prediction, learning, or editor-context model. The
 * caller supplies only the transient prefix entered through GoreeCloud Keyboard in the current
 * session. Matching is exact-prefix-only over a small static list and performs no I/O.
 */
internal object ArabicPrefixSuggestions {
    private val words = listOf(
        "من", "في", "على", "إلى", "عن", "مع",
        "هذا", "هذه", "ذلك", "تلك", "الذي", "التي", "الذين",
        "أنا", "أنت", "أنتم", "هو", "هي", "نحن", "هم",
        "كان", "كانت", "يكون", "تكون",
        "نعم", "لا", "ربما", "أيضا", "فقط",
        "كل", "بعض", "أكثر", "أقل", "قبل", "بعد", "بين", "عند", "حتى", "ثم", "لكن", "لأن",
        "إذا", "عندما", "كيف", "ماذا", "لماذا", "أين", "متى", "هل", "ما",
        "هنا", "هناك", "الآن", "اليوم", "غدا", "أمس",
        "شيء", "أشياء", "شخص", "أشخاص",
        "وقت", "يوم", "أيام", "سنة", "سنوات", "ساعة", "دقيقة", "دقائق",
        "مكان", "بيت", "منزل", "مدرسة", "جامعة",
        "كتاب", "كتب", "كلمة", "كلمات",
        "عمل", "أعمال", "فكرة", "أفكار", "سؤال", "أسئلة", "جواب", "إجابة",
        "مشكلة", "مشاكل", "حل", "حلول", "طريق", "طرق", "مدينة", "بلد", "عالم", "حياة",
        "ماء", "طعام", "سيارة", "هاتف", "حاسوب",
        "صورة", "صور", "فيديو", "رسالة", "رسائل", "ملف", "ملفات",
        "جديد", "جديدة", "جيد", "جيدة", "كبير", "كبيرة", "صغير", "صغيرة",
        "سريع", "سريعة", "مهم", "مهمة", "ممكن", "موجود", "موجودة",
        "صحيح", "صحيحة", "جميل", "جميلة",
        "مرحبا", "شكرا", "سلام", "صباح", "مساء",
        "أحب", "أريد", "أحتاج", "أعرف", "أفهم", "أعمل", "أكتب", "أقرأ",
        "أذهب", "أعود", "أرى", "أقول", "يمكن", "يجب", "يوجد", "لدي", "لديك", "لدينا",
    ).map(::normalize)

    fun suggest(prefix: String, limit: Int = 3): List<String> {
        if (limit <= 0) return emptyList()
        val normalizedPrefix = normalize(prefix.trim())
        if (normalizedPrefix.isEmpty() || !isArabicLettersOnly(normalizedPrefix)) return emptyList()

        val matches = words.asSequence()
            .filter { candidate -> candidate.startsWith(normalizedPrefix) }
            .distinct()
            .toList()

        val exact = matches.firstOrNull { it == normalizedPrefix }
        return buildList {
            exact?.let(::add)
            matches.forEach { candidate ->
                if (size >= limit) return@forEach
                if (candidate != exact) add(candidate)
            }
        }.take(limit)
    }

    private fun isArabicLettersOnly(value: String): Boolean =
        value.codePoints().allMatch { codePoint ->
            Character.isLetter(codePoint) &&
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.ARABIC
        }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
}
