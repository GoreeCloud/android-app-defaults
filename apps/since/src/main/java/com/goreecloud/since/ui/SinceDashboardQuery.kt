package com.goreecloud.since.ui

import com.goreecloud.since.domain.model.DashboardSort
import com.goreecloud.since.domain.model.TrackerAggregate
import java.text.Normalizer
import java.util.Locale

object SinceDashboardQuery {
    fun apply(
        aggregates: List<TrackerAggregate>,
        query: String,
        sort: DashboardSort,
    ): List<TrackerAggregate> {
        val needle = normalize(query.trim())
        val filtered = if (needle.isEmpty()) {
            aggregates
        } else {
            aggregates.filter { aggregate ->
                normalize(aggregate.tracker.title).contains(needle) ||
                    normalize(aggregate.tracker.note.orEmpty()).contains(needle)
            }
        }

        return when (sort) {
            DashboardSort.CREATED -> filtered.sortedWith(
                compareBy<TrackerAggregate> { it.tracker.sortOrder }
                    .thenBy { it.tracker.createdAtEpochMs }
                    .thenBy { normalize(it.tracker.title) }
                    .thenBy { it.tracker.id },
            )

            DashboardSort.TITLE -> filtered.sortedWith(
                compareBy<TrackerAggregate> { normalize(it.tracker.title) }
                    .thenByDescending { it.tracker.updatedAtEpochMs }
                    .thenBy { it.tracker.id },
            )

            DashboardSort.NEWEST_START -> filtered.sortedWith(
                compareByDescending<TrackerAggregate> { currentStartEpochMs(it) }
                    .thenBy { normalize(it.tracker.title) }
                    .thenBy { it.tracker.id },
            )

            DashboardSort.OLDEST_START,
            DashboardSort.LONGEST_CURRENT,
            -> filtered.sortedWith(
                compareBy<TrackerAggregate> { currentStartEpochMs(it) }
                    .thenBy { normalize(it.tracker.title) }
                    .thenBy { it.tracker.id },
            )
        }
    }

    private fun currentStartEpochMs(aggregate: TrackerAggregate): Long =
        aggregate.periods.single { it.endEpochMs == null }.startEpochMs

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .lowercase(Locale.ROOT)
}
