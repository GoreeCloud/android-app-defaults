package com.goreecloud.since.ui

import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SinceDashboardQueryTest {
    @Test
    fun searchMatchesTitleAndNoteCaseInsensitively() {
        val rows = listOf(
            aggregate("a", "Coffee", "Morning routine", 100),
            aggregate("b", "Exercise", "COFFEE break follows", 200),
            aggregate("c", "Reading", null, 300),
        )

        val result = SinceDashboardQuery.apply(rows, "coffee", SinceDashboardSort.RECENT)

        assertEquals(listOf("b", "a"), result.map { it.tracker.id })
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
    fun titleSortIsCaseInsensitiveAndStable() {
        val rows = listOf(
            aggregate("b", "beta", null, 300),
            aggregate("a", "Alpha", null, 100),
            aggregate("c", "alpha", null, 200),
        )

        val result = SinceDashboardQuery.apply(rows, "", SinceDashboardSort.TITLE)

        assertEquals(listOf("c", "a", "b"), result.map { it.tracker.id })
    }

    private fun aggregate(
        id: String,
        title: String,
        note: String?,
        updatedAt: Long,
    ): TrackerAggregate = TrackerAggregate(
        tracker = Tracker(
            id = id,
            title = title,
            note = note,
            kind = TrackerKind.PERMANENT,
            iconKey = null,
            accentKey = null,
            defaultDisplayFormat = DisplayFormat.DAYS,
            sortOrder = 0,
            isArchived = false,
            createdAtEpochMs = 1,
            updatedAtEpochMs = updatedAt,
        ),
        periods = emptyList(),
        goal = null,
    )
}
