package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiSearchSessionTest {
    @Test
    fun openAndTypeProducesLocalResults() {
        val session = EmojiSearchSession()
        session.open()
        val snapshot = session.append("travel")

        assertTrue(snapshot.active)
        assertEquals("travel", snapshot.query)
        assertTrue(snapshot.results.isNotEmpty())
        assertTrue(snapshot.results.all { it.category == EmojiCategory.TRAVEL })
    }

    @Test
    fun closedSessionDoesNotCollectTypedText() {
        val session = EmojiSearchSession()
        val snapshot = session.append("smile")

        assertFalse(snapshot.active)
        assertEquals("", snapshot.query)
        assertTrue(snapshot.results.isEmpty())
    }

    @Test
    fun closedSessionDoesNotAcceptReplacementQuery() {
        val session = EmojiSearchSession()

        val closed = session.replaceQuery("heart")
        val opened = session.open()

        assertFalse(closed.active)
        assertEquals("", closed.query)
        assertTrue(closed.results.isEmpty())
        assertTrue(opened.active)
        assertEquals("", opened.query)
        assertTrue(opened.results.isEmpty())
    }

    @Test
    fun backspaceRemovesOneCompleteCodePoint() {
        val session = EmojiSearchSession()
        session.open()
        session.replaceQuery("cat😀")
        val snapshot = session.backspace()

        assertEquals("cat", snapshot.query)
    }

    @Test
    fun queryIsBoundedByCodePointCount() {
        val session = EmojiSearchSession(maxQueryCodePoints = 4)
        session.open()
        val snapshot = session.replaceQuery("ab😀cd")

        assertEquals("ab😀c", snapshot.query)
        assertEquals(4, snapshot.query.codePointCount(0, snapshot.query.length))
    }

    @Test
    fun closeClearsTransientSearchState() {
        val session = EmojiSearchSession()
        session.open()
        session.append("heart")
        val snapshot = session.close()

        assertFalse(snapshot.active)
        assertEquals("", snapshot.query)
        assertTrue(snapshot.results.isEmpty())
    }
}
