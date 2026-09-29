// File internal version: 0.1.0
package com.goreecloud.camera.camera

enum class LensFacing {
    BACK,
    FRONT,
    EXTERNAL,
    UNKNOWN,
}

data class CameraDescriptor(
    val id: String,
    val lensFacing: LensFacing,
)
