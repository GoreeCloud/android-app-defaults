// File internal version: 0.1.0
package com.goreecloud.camera.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraSelectorTest {
    @Test
    fun selectsBackCameraBeforeOtherFacings() {
        val selected = CameraSelector.selectDefaultCamera(
            listOf(
                CameraDescriptor("front", LensFacing.FRONT),
                CameraDescriptor("external", LensFacing.EXTERNAL),
                CameraDescriptor("back", LensFacing.BACK),
            ),
        )

        assertEquals("back", selected?.id)
    }

    @Test
    fun selectionIsDeterministicWithinSameFacing() {
        val selected = CameraSelector.selectDefaultCamera(
            listOf(
                CameraDescriptor("2", LensFacing.BACK),
                CameraDescriptor("0", LensFacing.BACK),
            ),
        )

        assertEquals("0", selected?.id)
    }

    @Test
    fun returnsNullWhenNoCameraIsReported() {
        assertNull(CameraSelector.selectDefaultCamera(emptyList()))
    }
}
