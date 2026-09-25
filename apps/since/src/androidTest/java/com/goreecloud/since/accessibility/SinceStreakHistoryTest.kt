package com.goreecloud.since.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.goreecloud.since.domain.model.DisplayFormat
import com.goreecloud.since.domain.model.Tracker
import com.goreecloud.since.domain.model.TrackerAggregate
import com.goreecloud.since.domain.model.TrackerKind
import com.goreecloud.since.domain.model.TrackerPeriod
import com.goreecloud.since.testutil.FakeTrackerRepository
import com.goreecloud.since.ui.SinceApp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SinceStreakHistoryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-24T18:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    @Test
    fun resetPreservesHistoryAndExposesDerivedStatistics() {
        val repository = FakeTrackerRepository(
            initial = listOf(sampleAggregate()),
            clock = clock,
        )

        composeRule.setContent {
            MaterialTheme {
                SinceApp(
                    repository = repository,
                    clock = clock,
                )
            }
        }

        composeRule.onNodeWithText("Read daily").performClick()
        composeRule.onNodeWithText("Statistics").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset count").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("reset-streak").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "The current period will be preserved in History",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("reset-reason").performTextInput("Restarted plan")
        composeRule.onNodeWithTag("confirm-reset-streak").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val aggregate = repository.current.single()
            assertEquals(2, aggregate.periods.size)
            assertEquals(1, aggregate.periods.count { it.endEpochMs == null })
            assertEquals(
                "Restarted plan",
                aggregate.periods.single { it.endEpochMs != null }.resetReason,
            )
        }

        composeRule.onNodeWithTag("open-history").performScrollTo().performClick()
        composeRule.onNodeWithTag("history-screen").assertIsDisplayed()
        composeRule.onNodeWithText("1 completed periods").assertIsDisplayed()
        composeRule.onNodeWithText("Restarted plan").assertIsDisplayed()
        composeRule.onNodeWithText("Current").assertIsDisplayed()
    }

    private fun sampleAggregate(): TrackerAggregate {
        val trackerId = "tracker-history"
        return TrackerAggregate(
            tracker = Tracker(
                id = trackerId,
                title = "Read daily",
                note = "Keep the streak going.",
                kind = TrackerKind.STREAK,
                iconKey = null,
                accentKey = null,
                defaultDisplayFormat = DisplayFormat.DAYS,
                sortOrder = 0,
                isArchived = false,
                createdAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                updatedAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
            ),
            periods = listOf(
                TrackerPeriod(
                    id = "period-current",
                    trackerId = trackerId,
                    sequence = 0,
                    startEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                    startZoneId = "UTC",
                    endEpochMs = null,
                    endZoneId = null,
                    resetReason = null,
                    resetNote = null,
                    createdAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                    updatedAtEpochMs = now.minusSeconds(172_800).toEpochMilli(),
                )
            ),
            goal = null,
        )
    }
}