// File internal version: 0.1.0
package com.goreecloud.camera.camera

object CameraSelector {
    fun selectDefaultCamera(cameras: Collection<CameraDescriptor>): CameraDescriptor? =
        cameras
            .sortedWith(compareBy<CameraDescriptor>({ rank(it.lensFacing) }, { it.id }))
            .firstOrNull()

    private fun rank(lensFacing: LensFacing): Int = when (lensFacing) {
        LensFacing.BACK -> 0
        LensFacing.EXTERNAL -> 1
        LensFacing.FRONT -> 2
        LensFacing.UNKNOWN -> 3
    }
}
