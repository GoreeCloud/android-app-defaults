// File internal version: 0.1.0
package com.goreecloud.camera.guidance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraGuidancePolicyTest {
    @Test
    fun normalizeStepClampsPersistedValuesToWizardBounds() {
        assertEquals(0, CameraGuidancePolicy.normalizeStep(-10))
        assertEquals(1, CameraGuidancePolicy.normalizeStep(1))
        assertEquals(2, CameraGuidancePolicy.normalizeStep(99))
    }

    @Test
    fun navigationAdvancesAndStopsAtFinalStep() {
        assertEquals(1, CameraGuidancePolicy.nextStep(0))
        assertEquals(2, CameraGuidancePolicy.nextStep(1))
        assertNull(CameraGuidancePolicy.nextStep(2))
        assertEquals(0, CameraGuidancePolicy.previousStep(0))
        assertEquals(1, CameraGuidancePolicy.previousStep(2))
    }
}
