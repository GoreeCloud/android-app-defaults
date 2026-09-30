// File internal version: 0.1.0
package com.goreecloud.camera.storage

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object VideoFileNamer {
    private val formatter = DateTimeFormatter
        .ofPattern("yyyyMMdd_HHmmss_SSS")
        .withZone(ZoneOffset.UTC)

    fun displayName(epochMillis: Long = System.currentTimeMillis()): String =
        "GCAM_${formatter.format(Instant.ofEpochMilli(epochMillis))}.mp4"
}
