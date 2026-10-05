package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherAppIconNormalizationPolicyTest {
    @Test
    fun fullArtworkKeepsNaturalScale() {
        assertEquals(
            1f,
            launcherLegacyIconNormalizationScale(
                contentWidth = 132,
                contentHeight = 128,
                canvasSize = 144,
            ),
            0.0001f,
        )
    }

    @Test
    fun paddedLegacyArtworkGetsBoundedFillScale() {
        val scale = launcherLegacyIconNormalizationScale(
            contentWidth = 96,
            contentHeight = 92,
            canvasSize = 144,
        )

        assertTrue(scale > 1f)
        assertTrue(scale <= LAUNCHER_LEGACY_ICON_MAX_SCALE)
    }

    @Test
    fun extremePaddingNeverExceedsSafetyCap() {
        assertEquals(
            LAUNCHER_LEGACY_ICON_MAX_SCALE,
            launcherLegacyIconNormalizationScale(
                contentWidth = 36,
                contentHeight = 36,
                canvasSize = 144,
            ),
            0.0001f,
        )
    }

    @Test
    fun invalidGeometryFailsClosedToNaturalScale() {
        assertEquals(1f, launcherLegacyIconNormalizationScale(0, 0, 144), 0.0001f)
        assertEquals(1f, launcherLegacyIconNormalizationScale(72, 72, 0), 0.0001f)
    }
}
