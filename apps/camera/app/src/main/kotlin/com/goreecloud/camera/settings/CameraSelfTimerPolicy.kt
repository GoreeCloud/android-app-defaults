package com.goreecloud.camera.settings

object CameraSelfTimerPolicy {
    const val OFF_SECONDS = 0
    const val THREE_SECONDS = 3
    const val TEN_SECONDS = 10

    val supportedSeconds: List<Int> = listOf(
        OFF_SECONDS,
        THREE_SECONDS,
        TEN_SECONDS,
    )

    fun normalizeSeconds(seconds: Int): Int =
        if (seconds in supportedSeconds) seconds else OFF_SECONDS

    fun nextSeconds(current: Int): Int {
        val normalized = normalizeSeconds(current)
        val index = supportedSeconds.indexOf(normalized)
        return supportedSeconds[(index + 1) % supportedSeconds.size]
    }
}
