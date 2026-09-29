package com.goreecloud.clock.alarm

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmRepeatPresetPolicyTest {
    @Test
    fun presetsMapToStableDaySets() {
        assertEquals(
            emptySet<DayOfWeek>(),
            AlarmRepeatPresetPolicy.daysFor(AlarmRepeatPreset.ONE_TIME),
        )
        assertEquals(
            DayOfWeek.entries.toSet(),
            AlarmRepeatPresetPolicy.daysFor(AlarmRepeatPreset.EVERY_DAY),
        )
        assertEquals(
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            AlarmRepeatPresetPolicy.daysFor(AlarmRepeatPreset.WEEKDAYS),
        )
        assertEquals(
            setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            AlarmRepeatPresetPolicy.daysFor(AlarmRepeatPreset.WEEKENDS),
        )
    }

    @Test
    fun matchingPresetRecognizesExactPresetOnly() {
        AlarmRepeatPreset.entries.forEach { preset ->
            assertEquals(
                preset,
                AlarmRepeatPresetPolicy.matchingPreset(
                    AlarmRepeatPresetPolicy.daysFor(preset),
                ),
            )
        }

        assertNull(
            AlarmRepeatPresetPolicy.matchingPreset(
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            ),
        )
    }
}
