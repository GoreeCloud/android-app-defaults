// File internal version: 0.1.0
package com.goreecloud.camera.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoFileNamerTest {
    @Test
    fun usesStableUtcTimestampAndJpegExtension() {
        assertEquals(
            "GCAM_19700101_000000_000.jpg",
            PhotoFileNamer.displayName(0L),
        )
    }
}
