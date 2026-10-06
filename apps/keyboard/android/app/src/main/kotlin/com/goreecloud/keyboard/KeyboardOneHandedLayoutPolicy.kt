package com.goreecloud.keyboard

internal data class KeyboardHorizontalInsets(
    val leftPx: Float,
    val rightPx: Float,
)

internal object KeyboardOneHandedLayoutPolicy {
    private const val COMPACT_WIDTH_FRACTION = 0.82f

    fun horizontalInsets(
        totalWidthPx: Float,
        mode: KeyboardOneHandedMode,
    ): KeyboardHorizontalInsets {
        if (!totalWidthPx.isFinite() || totalWidthPx <= 0f || mode == KeyboardOneHandedMode.OFF) {
            return KeyboardHorizontalInsets(leftPx = 0f, rightPx = 0f)
        }

        val reservedPx = (totalWidthPx * (1f - COMPACT_WIDTH_FRACTION)).coerceAtLeast(0f)
        return when (mode) {
            KeyboardOneHandedMode.OFF ->
                KeyboardHorizontalInsets(leftPx = 0f, rightPx = 0f)
            KeyboardOneHandedMode.LEFT ->
                KeyboardHorizontalInsets(leftPx = 0f, rightPx = reservedPx)
            KeyboardOneHandedMode.RIGHT ->
                KeyboardHorizontalInsets(leftPx = reservedPx, rightPx = 0f)
        }
    }
}
