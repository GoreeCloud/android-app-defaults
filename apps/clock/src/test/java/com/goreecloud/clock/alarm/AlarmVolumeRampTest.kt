package com.goreecloud.clock.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmVolumeRampTest {
    @Test
    fun disabledRampUsesFullVolumeImmediately() {
        assertEquals(1f, AlarmVolumeRamp.volumeAt(0L, 0), 0.0001f)
    }

    @Test
    fun enabledRampStartsAudibleAndReachesFullVolume() {
        assertEquals(0.15f, AlarmVolumeRamp.volumeAt(0L, 30), 0.0001f)
        assertEquals(0.575f, AlarmVolumeRamp.volumeAt(15_000L, 30), 0.001f)
        assertEquals(1f, AlarmVolumeRamp.volumeAt(30_000L, 30), 0.0001f)
        assertEquals(1f, AlarmVolumeRamp.volumeAt(60_000L, 30), 0.0001f)
    }
}
