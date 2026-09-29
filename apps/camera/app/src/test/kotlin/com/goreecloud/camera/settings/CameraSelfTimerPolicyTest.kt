package com.goreecloud.camera.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraSelfTimerPolicyTest {
    @Test
    fun unsupportedStoredValuesFailClosedToOff() {
        assertEquals(CameraSelfTimerPolicy.OFF_SECONDS, CameraSelfTimerPolicy.normalizeSeconds(-1))
        assertEquals(CameraSelfTimerPolicy.OFF_SECONDS, CameraSelfTimerPolicy.normalizeSeconds(5))
    }

    @Test
    fun cycleIsBoundedToOffThreeAndTenSeconds() {
        assertEquals(3, CameraSelfTimerPolicy.nextSeconds(0))
        assertEquals(10, CameraSelfTimerPolicy.nextSeconds(3))
        assertEquals(0, CameraSelfTimerPolicy.nextSeconds(10))
        assertEquals(3, CameraSelfTimerPolicy.nextSeconds(99))
    }
}
