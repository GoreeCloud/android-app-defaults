package com.goreecloud.gallery.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrashTest {
    @Test
    fun restoreRequiresPlatformWriteGrant() {
        assertEquals(
            TrashDecision.Denied("platform-authorized media write access is required"),
            TrashPolicy.evaluate(TrashAction.RESTORE, hasPlatformWriteGrant = false, confirmed = false),
        )
    }

    @Test
    fun purgeRequiresExplicitConfirmation() {
        assertEquals(
            TrashDecision.Denied("permanent purge requires explicit confirmation"),
            TrashPolicy.evaluate(TrashAction.PURGE, hasPlatformWriteGrant = true, confirmed = false),
        )
        assertEquals(
            TrashDecision.Allowed,
            TrashPolicy.evaluate(TrashAction.PURGE, hasPlatformWriteGrant = true, confirmed = true),
        )
    }

    @Test
    fun trashEntryRequiresMediaIdentity() {
        assertFailsWith<IllegalArgumentException> {
            TrashEntry(mediaId = "", trashedAt = Instant.EPOCH)
        }
    }
}
