package com.goreecloud.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardOneHandedLayoutPolicyTest {
    @Test
    fun offKeepsFullWidth() {
        val insets = KeyboardOneHandedLayoutPolicy.horizontalInsets(
            totalWidthPx = 1000f,
            mode = KeyboardOneHandedMode.OFF,
        )

        assertEquals(0f, insets.leftPx, 0f)
        assertEquals(0f, insets.rightPx, 0f)
    }

    @Test
    fun leftAndRightModesReserveOnlyTheOppositeSide() {
        val left = KeyboardOneHandedLayoutPolicy.horizontalInsets(
            totalWidthPx = 1000f,
            mode = KeyboardOneHandedMode.LEFT,
        )
        val right = KeyboardOneHandedLayoutPolicy.horizontalInsets(
            totalWidthPx = 1000f,
            mode = KeyboardOneHandedMode.RIGHT,
        )

        assertEquals(0f, left.leftPx, 0f)
        assertEquals(180f, left.rightPx, 0.01f)
        assertEquals(180f, right.leftPx, 0.01f)
        assertEquals(0f, right.rightPx, 0f)
    }

    @Test
    fun invalidWidthsFailClosedToNoCompaction() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f).forEach { width ->
            val insets = KeyboardOneHandedLayoutPolicy.horizontalInsets(
                totalWidthPx = width,
                mode = KeyboardOneHandedMode.RIGHT,
            )
            assertTrue(insets.leftPx == 0f && insets.rightPx == 0f)
        }
    }
}
