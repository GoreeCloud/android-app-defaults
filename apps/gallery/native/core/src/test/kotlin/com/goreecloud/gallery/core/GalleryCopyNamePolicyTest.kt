package com.goreecloud.gallery.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GalleryCopyNamePolicyTest {
    @Test
    fun `copy names preserve file extensions`() {
        assertEquals(
            "IMG_0001 (copy).jpg",
            GalleryCopyNamePolicy.nextAvailable("IMG_0001.jpg", emptySet()),
        )
        assertEquals(
            "README (copy)",
            GalleryCopyNamePolicy.nextAvailable("README", emptySet()),
        )
    }

    @Test
    fun `copy names skip occupied case-insensitive variants`() {
        assertEquals(
            "IMG_0001 (copy 3).jpg",
            GalleryCopyNamePolicy.nextAvailable(
                "IMG_0001.jpg",
                setOf("img_0001 (COPY).jpg", "IMG_0001 (copy 2).jpg"),
            ),
        )
    }

    @Test
    fun `unsafe source display names fail closed`() {
        listOf("", "   ", "Trips/photo.jpg", "Trips\\photo.jpg", "bad\u0000name.jpg").forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                GalleryCopyNamePolicy.nextAvailable(value, emptySet())
            }
        }
    }
}
