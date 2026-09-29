// File internal version: 0.1.0
package com.goreecloud.camera.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFileNamerTest {
    @Test
    fun formatsUtcTimestampAsMp4Name() {
        assertEquals(
            "GCAM_20260102_030405_678.mp4",
            VideoFileNamer.displayName(1_767_323_045_678L),
        )
    }
}
