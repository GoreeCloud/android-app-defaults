package com.goreecloud.since.ui

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class SinceDashboardQueryTest {
    @Test
    fun searchMatchesTitleAndNoteCaseAndAccentInsensitively() {
        val rows = listOf(
            aggregate("a", "Café", "Morning routine", 100),
            aggregate("b", "Exercise", "CAFÉ break follows", 200),
            aggregate("c", "Reading", null, 300),
        )

        val result = SinceDashboardQuery.apply(rows, "cafe", SinceDashboardSort.TITLE)

        assertEquals(listOf("a", "b"), result.map { it.tracker.id })
    }

    @Test
    fun recentSortUsesUpdatedTimeWithDeterministicTieBreaks() {
        val rows = listOf(
            aggregate("b", "Zulu", null, 200),
            aggregate("c", "Alpha", null, 200),
            aggregate("a", "Older", null, 100),
        )

        val result = SinceDashboardQuery.apply(rows, "", SinceDashboardSort.RECENT)

        assertEquals(listOf("c", "b", "a"), result.map { it.tracker.id })
    }

    @Test
    fun createdSortUsesSortOrderThenCreationTime() {
        val rows = listOf(
            aggregate("b", "Second", null, 200, sortOrder = 1, createdAt = 300),
            aggregate("c", "Third", null, 300, sortOrder = 1, createdAt = 400),
            aggregate("a", "First", null, 100, sortOrder = 0, createdAt = 500),
        )

        val result = SinceDashboardQuery.apply(rows, "", SinceDashboardSort.CREATED)

        assertEquals(listOf("a", "b", "c"), result.map { it.tracker.id })
    }

    @Test
    fun titleSortIsCaseInsensitiveAndStable() {
        val rows = listOf(
            aggregate("b", "beta", null, 300),
            aggregate("a", "Alpha", null, 100),
            aggregate("c", "alpha", null, 200),
        )

        val result = SinceDashboardQuery.apply(rows, "", SinceDashboardSort.TITLE)

        assertEquals(listOf("c", "a", "b"), result.map { it.tracker.id })
    }

    @Test
    fun startBasedSortsAreDeterministic() {
        val rows = listOf(
            aggregate("middle", "Middle", null, 300, startEpochMs = 200),
            aggregate("newest", "Newest", null, 100, startEpochMs = 300),
            aggregate("oldest", "Oldest", null, 200, startEpochMs = 100),
        )

        assertEquals(
            listOf("newest", "middle", "oldest"),
            SinceDashboardQuery.apply(rows, "", SinceDashboardSort.NEWEST_START)
                .map { it.tracker.id },
        )
        assertEquals(
            listOf("oldest", "middle", "newest"),
            SinceDashboardQuery.apply(rows, "", SinceDashboardSort.OLDEST_START)
                .map { it.tracker.id },
        )
        assertEquals(
            listOf("oldest", "middle", "newest"),
            SinceDashboardQuery.apply(rows, "", SinceDashboardSort.LONGEST_CURRENT)
                .map { it.tracker.id },
        )
    }

    private fun aggregate(
        id: String,
        title: String,
        note: String?,
        updatedAt: Long,
        sortOrder: Int = 0,
        createdAt: Long = 1,
        startEpochMs: Long = 0,
    ): TrackerAggregate = TrackerAggregate(
        tracker = Tracker(
            id = id,
            title = title,
            note = note,
            kind = TrackerKind.EVENT,
            iconKey = null,
            accentKey = null,
            defaultDisplayFormat = DisplayFormat.DAYS,
            sortOrder = sortOrder,
            isArchived = false,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = updatedAt,
        ),
        periods = listOf(
            TrackerPeriod(
                id = "$id-period",
                trackerId = id,
                sequence = 0,
                startEpochMs = startEpochMs,
                startZoneId = "UTC",
                endEpochMs = null,
                endZoneId = null,
                resetReason = null,
                resetNote = null,
                createdAtEpochMs = createdAt,
                updatedAtEpochMs = updatedAt,
            ),
        ),
        goal = null,
    )
}
