package com.goreecloud.since.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Goal
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.domain.repository.TrackerRepository
import com.goreecloud.since.domain.validation.ValidatedTrackerDraft
import com.goreecloud.since.ui.SinceApp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test

class SinceAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clock = Clock.fixed(
        Instant.parse("2026-09-24T05:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Test
    fun trackerCardIsOneCoherentClickableAccessibleUnit() {
        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = FakeTrackerRepository(listOf(sampleAggregate())),
                    clock = clock,
                )
            }
        }

        composeRule
            .onNodeWithText("Since")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeRule
            .onNodeWithText("Read daily")
            .assertHasClickAction()

        composeRule
            .onNodeWithText("Goal: 30 Days")
            .assertHasClickAction()
    }

    @Test
    fun editorHeadingsRadioRowsAndValidationErrorsExposeAccessibleSemantics() {
        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = FakeTrackerRepository(emptyList()),
                    clock = clock,
                )
            }
        }

        composeRule.onNodeWithText("Add tracker").performClick()
        composeRule.onNodeWithText("Permanent Event").performClick()

        composeRule
            .onNodeWithText("Create Permanent Event")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeRule
            .onNodeWithText("Days")
            .assertHasClickAction()

        composeRule
            .onNodeWithText("Save")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithText("Title must contain between 1 and 80 Unicode characters.")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Assertive,
                )
            )
    }

    private fun sampleAggregate(): TrackerAggregate {
        val trackerId = "tracker-accessibility"
        return TrackerAggregate(
            tracker = Tracker(
                id = trackerId,
                title = "Read daily",
                note = "Keep going.",
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 0,
                isArchived = false,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "period-accessibility",
                    trackerId = trackerId,
                    sequence = 0,
                    startEpochMs = Instant.parse("2026-09-23T05:00:00Z").toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = 1_000L,
                    updatedAtEpochMs = 1_000L,
                )
            ),
            goal = Goal(
                trackerId = trackerId,
                targetAmount = 30,
                targetUnit = DisplayFormat.DAYS,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        )
    }
}

private class FakeTrackerRepository(
    initial: List<TrackerAggregate>,
) : TrackerRepository {
    private val aggregates = MutableStateFlow(initial)

    override fun observeActiveTrackers(): Flow<List<Tracker>> =
        aggregates.map { values -> values.map { it.tracker } }

    override fun observeActiveTrackerAggregates(): Flow<List<TrackerAggregate>> = aggregates

    override suspend fun createTracker(draft: ValidatedTrackerDraft): TrackerAggregate =
        error("Creation is not expected in this accessibility test.")

    override suspend fun loadTracker(trackerId: String): TrackerAggregate? =
        aggregates.value.firstOrNull { it.tracker.id == trackerId }

    override suspend fun updateTracker(
        trackerId: String,
        draft: ValidatedTrackerDraft,
    ): TrackerAggregate? =
        error("Update is not expected in this accessibility test.")

    override suspend fun updateDisplayFormat(
        trackerId: String,
        displayFormat: DisplayFormat,
    ): Boolean = false
}
