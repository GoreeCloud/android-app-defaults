package com.goreecloud.since.ui

import com.goreecloud.since.domain.model.TrackerAggregate
import java.util.Locale

enum class SinceDashboardSort {
    RECENT,
    TITLE,
}

object SinceDashboardQuery {
    fun apply(
        aggregates: List<TrackerAggregate>,
        query: String,
        sort: SinceDashboardSort,
    ): List<TrackerAggregate> {
        val needle = query.trim().lowercase(Locale.ROOT)
        val filtered = if (needle.isEmpty()) {
            aggregates
        } else {
            aggregates.filter { aggregate ->
                aggregate.tracker.title.lowercase(Locale.ROOT).contains(needle) ||
                    aggregate.tracker.note.orEmpty().lowercase(Locale.ROOT).contains(needle)
            }
        }

        return when (sort) {
            SinceDashboardSort.RECENT -> filtered.sortedWith(
                compareByDescending<TrackerAggregate> { it.tracker.updatedAtEpochMs }
                    .thenBy { it.tracker.title.lowercase(Locale.ROOT) }
                    .thenBy { it.tracker.id },
            )

            SinceDashboardSort.TITLE -> filtered.sortedWith(
                compareBy<TrackerAggregate> { it.tracker.title.lowercase(Locale.ROOT) }
                    .thenByDescending { it.tracker.updatedAtEpochMs }
                    .thenBy { it.tracker.id },
            )
        }
    }
}
