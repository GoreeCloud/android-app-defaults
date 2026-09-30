package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyAlternatesTest {
    @Test
    fun lowerCaseLettersExposeDeterministicLocalAlternates() {
        assertEquals(
            listOf("á", "à", "â", "ä", "ã", "å", "æ"),
            KeyAlternates.forKey("a"),
        )
        assertEquals(listOf("ñ"), KeyAlternates.forKey("n"))
    }

    @Test
    fun upperCaseLettersReceiveUpperCaseAlternates() {
        assertEquals(
            listOf("Á", "À", "Â", "Ä", "Ã", "Å", "Æ"),
            KeyAlternates.forKey("A"),
        )
    }

    @Test
    fun punctuationAlternatesRemainPunctuation() {
        assertEquals(listOf("…"), KeyAlternates.forKey("."))
        assertEquals(listOf("–", "—"), KeyAlternates.forKey("-"))
        assertEquals(listOf("¿"), KeyAlternates.forKey("?"))
    }

    @Test
    fun unsupportedAndEmptyKeysFailClosed() {
        assertTrue(KeyAlternates.forKey("").isEmpty())
        assertTrue(KeyAlternates.forKey("q").isEmpty())
        assertTrue(KeyAlternates.forKey("context-dependent").isEmpty())
    }
}
