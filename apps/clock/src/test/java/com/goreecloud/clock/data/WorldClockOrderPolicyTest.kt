package com.goreecloud.clock.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldClockOrderPolicyTest {
    @Test
    fun normalizePreservesFirstValidOccurrenceAndDropsMalformedZones() {
        assertEquals(
            listOf("Europe/London", "UTC", "Asia/Tokyo"),
            WorldClockOrderPolicy.normalize(
                listOf(
                    " Europe/London ",
                    "not/a-zone",
                    "UTC",
                    "Europe/London",
                    "",
                    "Asia/Tokyo",
                )
            ),
        )
    }

    @Test
    fun addRemoveAndMovePreserveDeterministicOrder() {
        val initial = listOf("Europe/London", "Asia/Tokyo", "America/New_York")

        assertEquals(
            listOf("Europe/London", "Asia/Tokyo", "America/New_York", "Africa/Lagos"),
            WorldClockOrderPolicy.add(initial, "Africa/Lagos"),
        )
        assertEquals(
            listOf("Europe/London", "America/New_York"),
            WorldClockOrderPolicy.remove(initial, "Asia/Tokyo"),
        )
        assertEquals(
            listOf("Asia/Tokyo", "Europe/London", "America/New_York"),
            WorldClockOrderPolicy.move(initial, index = 1, offset = -1),
        )
        assertEquals(
            listOf("Europe/London", "America/New_York", "Asia/Tokyo"),
            WorldClockOrderPolicy.move(initial, index = 1, offset = 1),
        )
    }

    @Test
    fun invalidMoveLeavesNormalizedOrderUnchanged() {
        assertEquals(
            listOf("Europe/London", "Asia/Tokyo"),
            WorldClockOrderPolicy.move(
                listOf("Europe/London", "bad-zone", "Asia/Tokyo"),
                index = 0,
                offset = -1,
            ),
        )
    }
}
