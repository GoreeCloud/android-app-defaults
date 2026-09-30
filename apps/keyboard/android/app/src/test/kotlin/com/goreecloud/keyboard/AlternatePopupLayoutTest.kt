package com.goreecloud.keyboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlternatePopupLayoutTest {
    @Test
    fun `places common alternate row above source when space is available`() {
        val result = AlternatePopupLayout.calculate(
            source = AlternatePopupSourceBounds(180f, 320f, 260f, 380f),
            itemCount = 4,
            viewportWidth = 440f,
            viewportHeight = 520f,
            cellSize = 48f,
            gap = 8f,
        )

        assertEquals(4, result.columns)
        assertEquals(1, result.rows)
        assertTrue(result.placedAboveSource)
        assertTrue(result.left >= 8f)
        assertTrue(result.left + result.contentWidth <= 432f)
        assertTrue(result.top >= 8f)

        val first = result.itemBounds(0)
        val last = result.itemBounds(3)
        assertEquals(result.left, first.left)
        assertEquals(result.top, first.top)
        assertEquals(result.left + result.contentWidth, last.right)
        assertEquals(result.top + result.cellSize, last.bottom)
    }

    @Test
    fun `moves popup below a top-row source`() {
        val result = AlternatePopupLayout.calculate(
            source = AlternatePopupSourceBounds(100f, 12f, 160f, 72f),
            itemCount = 3,
            viewportWidth = 360f,
            viewportHeight = 420f,
            cellSize = 48f,
            gap = 8f,
        )

        assertFalse(result.placedAboveSource)
        assertEquals(80f, result.top)
    }

    @Test
    fun `reduces columns and exposes bounded multi-row item geometry`() {
        val result = AlternatePopupLayout.calculate(
            source = AlternatePopupSourceBounds(80f, 240f, 140f, 300f),
            itemCount = 6,
            viewportWidth = 176f,
            viewportHeight = 520f,
            cellSize = 48f,
            gap = 8f,
        )

        assertEquals(3, result.columns)
        assertEquals(2, result.rows)
        assertTrue(result.left + result.contentWidth <= 168f)

        val fourth = result.itemBounds(3)
        val sixth = result.itemBounds(5)
        assertEquals(result.left, fourth.left)
        assertEquals(result.top + result.cellSize + result.gap, fourth.top)
        assertEquals(result.left + result.contentWidth, sixth.right)
        assertTrue(sixth.bottom <= 512f)
        assertFailsWith<IllegalArgumentException> { result.itemBounds(6) }
    }

    @Test
    fun `hit testing selects cells but never gaps or unused grid space`() {
        val result = AlternatePopupLayout.calculate(
            source = AlternatePopupSourceBounds(80f, 240f, 140f, 300f),
            itemCount = 5,
            viewportWidth = 176f,
            viewportHeight = 520f,
            cellSize = 48f,
            gap = 8f,
        )

        val first = result.itemBounds(0)
        val fifth = result.itemBounds(4)
        assertEquals(0, result.hitTest(first.left + 1f, first.top + 1f))
        assertEquals(4, result.hitTest(fifth.right - 1f, fifth.bottom - 1f))
        assertNull(result.hitTest(first.right + 2f, first.top + 10f))

        val unusedThirdColumnX = result.left + 2 * (result.cellSize + result.gap) + 1f
        val secondRowY = result.top + result.cellSize + result.gap + 1f
        assertNull(result.hitTest(unusedThirdColumnX, secondRowY))
        assertNull(result.hitTest(Float.NaN, first.top))
        assertNull(result.hitTest(result.left - 1f, result.top))
    }

    @Test
    fun `fails closed when viewport cannot fit one cell or popup height`() {
        assertFailsWith<IllegalArgumentException> {
            AlternatePopupLayout.calculate(
                source = AlternatePopupSourceBounds(1f, 1f, 20f, 20f),
                itemCount = 1,
                viewportWidth = 40f,
                viewportHeight = 100f,
                cellSize = 48f,
                gap = 8f,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AlternatePopupLayout.calculate(
                source = AlternatePopupSourceBounds(40f, 40f, 90f, 90f),
                itemCount = 10,
                viewportWidth = 120f,
                viewportHeight = 160f,
                cellSize = 48f,
                gap = 8f,
            )
        }
    }
}
