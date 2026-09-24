package com.goreecloud.since.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.domain.repository.TrackerRepository
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import com.goreecloud.since.ui.theme.SinceTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SinceAppAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-23T18:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    @Test
    fun dashboardTrackerCardIsOneAccessibleClickableUnit() {
        val repository = FakeTrackerRepository(
            clock = clock,
            initial = listOf(
                trackerAggregate(
                    id = "accessibility",
                    title = "Read daily",
                    kind = TrackerKind.STREAK,
                    startEpochMs = now.minusSeconds(86_400).toEpochMilli(),
                    goal = Goal(
                        trackerId = "accessibility",
                        targetAmount = 30,
                        targetUnit = DisplayFormat.DAYS,
                        createdAtEpochMs = now.toEpochMilli(),
                        updatedAtEpochMs = now.toEpochMilli(),
                    ),
                )
            ),
        )

        composeRule.setContent {
            SinceTheme {
                SinceApp(repository = repository, clock = clock)
            }
        }

        composeRule
            .onNodeWithTag("tracker-card-accessibility")
            .assertHasClickAction()
            .assertTextContains("Read daily")
            .assertTextContains("Streak")
            .assertTextContains("Goal: 30 Days")
    }

    @Test
    fun displayFormatLabelIsPartOfFullWidthRadioTarget() {
        val repository = FakeTrackerRepository(
            clock = clock,
            initial = listOf(
                trackerAggregate(
                    id = "formats",
                    title = "First car",
                    kind = TrackerKind.EVENT,
                    startEpochMs = now.minusSeconds(3_600).toEpochMilli(),
                )
            ),
        )

        composeRule.setContent {
            SinceTheme {
                SinceApp(repository = repository, clock = clock)
            }
        }

        composeRule.onNodeWithTag("tracker-card-formats").performClick()
        composeRule
            .onNode(hasText("Months") and hasClickAction())
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()
        assertEquals(
            DisplayFormat.MONTHS,
            repository.current.single().tracker.defaultDisplayFormat,
        )
    }

    @Test
    fun createCustomPastStartThenEditPersistsThroughRepositoryFlow() {
        val repository = FakeTrackerRepository(clock = clock)

        composeRule.setContent {
            SinceTheme {
                SinceApp(repository = repository, clock = clock)
            }
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        composeRule.onNodeWithTag("title-field").performTextInput("First car")
        composeRule.onNodeWithTag("start-date-time-field").performTextClearance()
        composeRule.onNodeWithTag("start-date-time-field").performTextInput("2026-09-20 12:30")
        composeRule.onNodeWithTag("start-zone-field").performTextClearance()
        composeRule.onNodeWithTag("start-zone-field").performTextInput("America/Chicago")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("First car").assertIsDisplayed()
        composeRule.onNodeWithText("America/Chicago").assertIsDisplayed()

        composeRule.onNodeWithText("Edit").performClick()
        composeRule.onNodeWithTag("title-field").performTextClearance()
        composeRule.onNodeWithTag("title-field").performTextInput("First car edited")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithText("First car edited").assertIsDisplayed()
        assertEquals("First car edited", repository.current.single().tracker.title)
        assertEquals(
            ZoneId.of("America/Chicago").id,
            repository.current.single().periods.single().startZoneId,
        )
    }

    private fun trackerAggregate(
        id: String,
        title: String,
        kind: TrackerKind,
        startEpochMs: Long,
        goal: Goal? = null,
    ): TrackerAggregate {
        val timestamp = now.toEpochMilli()
        return TrackerAggregate(
            tracker = Tracker(
                id = id,
                title = title,
                note = null,
                kind = kind,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 0,
                isArchived = false,
                createdAtEpochMs = timestamp,
                updatedAtEpochMs = timestamp,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = id + "-period",
                    trackerId = id,
                    sequence = 0,
                    startEpochMs = startEpochMs,
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = timestamp,
                    updatedAtEpochMs = timestamp,
                )
            ),
            goal = goal,
        )
    }

    private class FakeTrackerRepository(
        private val clock: Clock,
        initial: List<TrackerAggregate> = emptyList(),
    ) : TrackerRepository {
        private val aggregates = MutableStateFlow(initial)
        private var nextId = initial.size + 1

        val current: List<TrackerAggregate>
            get() = aggregates.value

        override fun observeActiveTrackers(): Flow<List<Tracker>> =
            aggregates.map { rows -> rows.map { it.tracker } }

        override fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>> =
            aggregates

        override suspend fun createTracker(
            draft: ValidatedTrackerDraft,
        ): TrackerAggregate {
            val id = "created-" + nextId++
            val timestamp = clock.millis()
            val created = TrackerAggregate(
                tracker = Tracker(
                    id = id,
                    title = draft.title,
                    note = draft.note,
                    kind = draft.kind,
                    iconKey = null,
                    accentKey = null,
                    defaultDisplayFormat = draft.displayFormat,
                    sortOrder = aggregates.value.size,
                    isArchived = false,
                    createdAtEpochMs = timestamp,
                    updatedAtEpochMs = timestamp,
                ),
                periods = listOf(
                    TrackerPeriod(
                        id = id + "-period",
                        trackerId = id,
                        sequence = 0,
                        startEpochMs = draft.startEpochMs,
                        startZoneId = draft.startZoneId,
                        endEpochMs = null,
                        endZoneId = null,
                        resetReason = null,
                        resetNote = null,
                        createdAtEpochMs = timestamp,
                        updatedAtEpochMs = timestamp,
                    )
                ),
                goal = draft.goalAmount?.let { amount ->
                    Goal(
                        trackerId = id,
                        targetAmount = amount,
                        targetUnit = checkNotNull(draft.goalUnit),
                        createdAtEpochMs = timestamp,
                        updatedAtEpochMs = timestamp,
                    )
                },
            )
            aggregates.value = aggregates.value + created
            return created
        }

        override suspend fun loadTracker(trackerId: String): TrackerAggregate? =
            aggregates.value.firstOrNull { it.tracker.id == trackerId }

        override suspend fun updateTracker(
            trackerId: String,
            draft: ValidatedTrackerDraft,
        ): TrackerAggregate? {
            val existing = loadTracker(trackerId) ?: return null
            if (existing.tracker.kind != draft.kind) return null
            val timestamp = clock.millis()
            val currentPeriod = existing.periods.single { it.endEpochMs == null }
            val updated = existing.copy(
                tracker = existing.tracker.copy(
                    title = draft.title,
                    note = draft.note,
                    defaultDisplayFormat = draft.displayFormat,
                    updatedAtEpochMs = timestamp,
                ),
                periods = existing.periods.map { period ->
                    if (period.id == currentPeriod.id) {
                        period.copy(
                            startEpochMs = draft.startEpochMs,
                            startZoneId = draft.startZoneId,
                            updatedAtEpochMs = timestamp,
                        )
                    } else {
                        period
                    }
                },
            )
            aggregates.value = aggregates.value.map { row ->
                if (row.tracker.id == trackerId) updated else row
            }
            return updated
        }

        override suspend fun updateDisplayFormat(
            trackerId: String,
            displayFormat: DisplayFormat,
        ): Boolean {
            val existing = loadTracker(trackerId) ?: return false
            val updated = existing.copy(
                tracker = existing.tracker.copy(
                    defaultDisplayFormat = displayFormat,
                    updatedAtEpochMs = clock.millis(),
                )
            )
            aggregates.value = aggregates.value.map { row ->
                if (row.tracker.id == trackerId) updated else row
            }
            return true
        }
    }
}
