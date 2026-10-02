package com.goreecloud.keyboard

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLanguageTest {
    @After
    fun restoreEnglishLayout() {
        KeyboardLayout.activateLanguage(KeyboardLanguage.ENGLISH_US)
    }

    @Test
    fun androidSubtypeLocaleIsTheLanguageAuthority() {
        assertEquals(KeyboardLanguage.ENGLISH_US, KeyboardLanguage.fromSubtypeLocale("en_US"))
        assertEquals(KeyboardLanguage.ENGLISH_US, KeyboardLanguage.fromSubtypeLocale("en-US"))
        assertEquals(KeyboardLanguage.ARABIC, KeyboardLanguage.fromSubtypeLocale("ar"))
        assertEquals(KeyboardLanguage.ARABIC, KeyboardLanguage.fromSubtypeLocale("ar_EG"))
        assertEquals(KeyboardLanguage.ENGLISH_US, KeyboardLanguage.fromSubtypeLocale(null))
        assertEquals(KeyboardLanguage.ENGLISH_US, KeyboardLanguage.fromSubtypeLocale("unsupported"))
    }

    @Test
    fun arabicProfileIsRtlAndDoesNotReuseEnglishAssistance() {
        val language = KeyboardLanguage.ARABIC

        assertEquals(KeyboardWritingDirection.RIGHT_TO_LEFT, language.writingDirection)
        assertFalse(language.supportsCaseShift)
        assertFalse(language.supportsLocalEnglishAssistance)
        assertEquals("العربية", language.spacebarLabel)
    }

    @Test
    fun arabicSubtypeSelectsArabicLettersAndDigits() {
        KeyboardLayout.activateLanguage(KeyboardLanguage.ARABIC)

        assertEquals(KeyboardLanguage.ARABIC, KeyboardLayout.currentLanguage())
        assertEquals(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"),
            KeyboardLayout.characterRows(KeyboardLayer.LETTERS).first(),
        )
        assertEquals(
            listOf("١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "٠"),
            KeyboardLayout.numberRow(),
        )
    }

    @Test
    fun arabicSubtypeUsesArabicPunctuationWithoutChangingSharedSymbols() {
        KeyboardLayout.activateLanguage(KeyboardLanguage.ARABIC)

        val symbols = KeyboardLayout.characterRows(KeyboardLayer.SYMBOLS).flatten()
        assertEquals(listOf("،", "."), KeyboardLayout.letterPunctuation())
        assertTrue(symbols.contains("؟"))
        assertTrue(symbols.contains("؛"))
        assertTrue(symbols.contains("،"))
        assertTrue(symbols.contains("٪"))
        assertFalse(symbols.contains("?"))
        assertFalse(symbols.contains(";"))
    }

    @Test
    fun englishSubtypeKeepsExistingQwertyFoundation() {
        KeyboardLayout.activateLanguage(KeyboardLanguage.ENGLISH_US)

        assertEquals(KeyboardWritingDirection.LEFT_TO_RIGHT, KeyboardLanguage.ENGLISH_US.writingDirection)
        assertTrue(KeyboardLanguage.ENGLISH_US.supportsCaseShift)
        assertTrue(KeyboardLanguage.ENGLISH_US.supportsLocalEnglishAssistance)
        assertEquals(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            KeyboardLayout.characterRows(KeyboardLayer.LETTERS).first(),
        )
        assertEquals(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            KeyboardLayout.numberRow(),
        )
    }
}
