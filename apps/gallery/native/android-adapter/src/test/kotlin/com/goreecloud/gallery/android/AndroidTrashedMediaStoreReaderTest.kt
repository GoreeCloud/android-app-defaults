package com.goreecloud.gallery.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidTrashedMediaStoreReaderTest {
    @Test
    fun `trash browsing requires Android 11 or newer`() {
        assertFalse(AndroidTrashedMediaStoreReader.isSupported(29))
        assertTrue(AndroidTrashedMediaStoreReader.isSupported(30))
        assertTrue(AndroidTrashedMediaStoreReader.isSupported(36))
    }

    @Test
    fun `trash reader remains bounded`() {
        assertEquals(30, AndroidTrashedMediaStoreReader.MIN_SUPPORTED_API)
        assertEquals(250, AndroidTrashedMediaStoreReader.DEFAULT_MAX_ROWS)
        assertEquals(500, AndroidTrashedMediaStoreReader.MAX_ROWS)
    }
}
