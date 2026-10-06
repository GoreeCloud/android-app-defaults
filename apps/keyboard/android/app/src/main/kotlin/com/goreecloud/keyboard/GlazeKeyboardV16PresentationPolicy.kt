package com.goreecloud.keyboard

/**
 * Bounded repository-local presentation mapping for current Stable GLAZE UI V1.6.
 *
 * The existing V1.2 neutral optical palette remains the implemented visual substrate while this
 * policy introduces current V1.6 accessibility/motion context at the Android runtime boundary.
 * It does not create privacy, security, authorization, connectivity, recovery, or input truth.
 */
internal data class GlazeKeyboardV16PresentationSignals(
    val fontScale: Float,
    val animationsEnabled: Boolean,
    val touchExplorationEnabled: Boolean,
)

internal data class GlazeKeyboardV16PresentationContext(
    val reducedMotion: Boolean = false,
    val largeText: Boolean = false,
    val extraLargeText: Boolean = false,
    val touchAssistance: Boolean = false,
    val screenReaderOptimized: Boolean = false,
)

internal enum class GlazeKeyboardV16MotionMode {
    STANDARD,
    MINIMAL,
}

internal object GlazeKeyboardV16AndroidPresentationContext {
    const val LARGE_TEXT_FONT_SCALE = 1.30f
    const val EXTRA_LARGE_TEXT_FONT_SCALE = 1.60f

    fun resolve(signals: GlazeKeyboardV16PresentationSignals): GlazeKeyboardV16PresentationContext =
        GlazeKeyboardV16PresentationContext(
            reducedMotion = !signals.animationsEnabled,
            largeText = signals.fontScale >= LARGE_TEXT_FONT_SCALE,
            extraLargeText = signals.fontScale >= EXTRA_LARGE_TEXT_FONT_SCALE,
            touchAssistance = signals.touchExplorationEnabled,
            screenReaderOptimized = signals.touchExplorationEnabled,
        )
}

internal object GlazeKeyboardV16PresentationPolicy {
    const val MappedVersion = "1.6.0"
    const val MappedSourceRevision = "a7180679ea851389e0f3004515f9a25f420e716d"

    // The current Keyboard substrate remains the proven V1.2 implementation until a separately
    // accepted optical migration replaces it.
    const val InheritedOpticalVersion = "1.2.0"
    const val InheritedOpticalSourceRevision = "f285b9145e27e6e7027b075c37299d101945c272"

    private const val OrdinaryPreferredImeHeightDp = 364f
    private const val KeyboardRowCount = 5f
    private const val TopInteractionRowCount = 2f
    private const val VerticalGapCount = 5f

    fun interactionFloorDp(context: GlazeKeyboardV16PresentationContext): Float =
        GlazeKeyboardTokens.interactionFloorDp(context.touchAssistance)

    fun preferredImeHeightDp(context: GlazeKeyboardV16PresentationContext): Float {
        val interactionFloorDp = interactionFloorDp(context)
        return maxOf(
            OrdinaryPreferredImeHeightDp,
            interactionFloorDp * TopInteractionRowCount +
                GlazeKeyboardTokens.Space2Dp +
                GlazeKeyboardTokens.Space1Dp * VerticalGapCount +
                interactionFloorDp * KeyboardRowCount,
        )
    }

    /**
     * Compress inter-row whitespace before ordinary key rows fall below the current interaction
     * floor when an IME host constrains the requested height. The caller supplies only the vertical
     * space available to key rows and their gaps, so toolbar/suggestion/system-inset accounting
     * stays owned by the rendered surface.
     */
    fun verticalGapDp(
        availableRowsHeightDp: Float,
        rowCount: Int,
        context: GlazeKeyboardV16PresentationContext,
    ): Float {
        if (rowCount <= 1 || availableRowsHeightDp <= 0f) return 0f
        val interactionFloorDp = interactionFloorDp(context)
        val remainingForGapsDp = (
            availableRowsHeightDp - interactionFloorDp * rowCount
        ).coerceAtLeast(0f)
        return (remainingForGapsDp / (rowCount - 1))
            .coerceAtMost(GlazeKeyboardTokens.Space1Dp)
    }

    fun motionMode(context: GlazeKeyboardV16PresentationContext): GlazeKeyboardV16MotionMode =
        if (context.reducedMotion) GlazeKeyboardV16MotionMode.MINIMAL
        else GlazeKeyboardV16MotionMode.STANDARD
}
