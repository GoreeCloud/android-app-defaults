package com.goreecloud.clock.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepTest {
    @Test
    fun stableIdsRoundTripAndUnknownValuesFailSafeToWelcome() {
        OnboardingStep.entries.forEach { step ->
            assertEquals(step, OnboardingStep.fromId(step.id))
        }
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.fromId("future-step"))
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.fromId(null))
    }
}
