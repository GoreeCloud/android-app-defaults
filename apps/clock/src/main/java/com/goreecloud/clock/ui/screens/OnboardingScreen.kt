package com.goreecloud.clock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goreecloud.clock.data.ClockFacePreference
import com.goreecloud.clock.data.ClockPreferences
import com.goreecloud.clock.data.ClockPreferencesStore
import com.goreecloud.clock.data.OnboardingStep

@Composable
fun OnboardingScreen(
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
) {
    val step = preferences.onboardingStep
    val index = OnboardingStep.entries.indexOf(step).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "GoreeCloud Clock",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Step ${index + 1} of ${OnboardingStep.entries.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (step) {
            OnboardingStep.WELCOME -> WelcomeStep()
            OnboardingStep.TIME_DISPLAY -> TimeDisplayStep(preferences, preferencesStore)
            OnboardingStep.GUIDANCE -> GuidanceStep(
                preferences = preferences,
                preferencesStore = preferencesStore,
                exactAlarmAccess = exactAlarmAccess,
                notificationAccess = notificationAccess,
            )
            OnboardingStep.READY -> ReadyStep(exactAlarmAccess, notificationAccess)
        }

        Spacer(Modifier.weight(1f, fill = false))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (index > 0) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        preferencesStore.setOnboardingStep(OnboardingStep.entries[index - 1])
                    },
                ) {
                    Text("Back")
                }
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (step == OnboardingStep.READY) {
                        preferencesStore.completeOnboarding()
                    } else {
                        preferencesStore.setOnboardingStep(OnboardingStep.entries[index + 1])
                    }
                },
            ) {
                Text(if (step == OnboardingStep.READY) "Start using Clock" else "Continue")
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Welcome to GoreeCloud Clock",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Keep time, alarms, timers, stopwatch laps, and world clocks on your device with no GoreeCloud account and no network dependency.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReadinessCard(
            title = "Private by default",
            body = "Clock has no Internet permission. Core time-management data stays in app-private local storage.",
        )
        ReadinessCard(
            title = "Independent",
            body = "Clock works on its own and does not require another GoreeCloud application.",
        )
    }
}

@Composable
private fun TimeDisplayStep(
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Time & display",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "These are optional preferences. You can change them later in Clock Settings.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("24-hour time", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Use 00:00–23:59 instead of AM/PM.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = preferences.use24Hour,
                onCheckedChange = preferencesStore::setUse24Hour,
            )
        }
        Text("Default clock face", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = preferences.clockFace == ClockFacePreference.DIGITAL,
                onClick = { preferencesStore.setClockFace(ClockFacePreference.DIGITAL) },
                label = { Text("Digital") },
            )
            FilterChip(
                selected = preferences.clockFace == ClockFacePreference.ANALOG,
                onClick = { preferencesStore.setClockFace(ClockFacePreference.ANALOG) },
                label = { Text("Analog") },
            )
        }
    }
}

@Composable
private fun GuidanceStep(
    preferences: ClockPreferences,
    preferencesStore: ClockPreferencesStore,
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Guidance & permissions",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text("Permissions stay contextual", style = MaterialTheme.typography.titleMedium)
        Text(
            "Clock does not ask for alarm or notification access just because you opened the app. Android permission prompts are shown near alarm and timer features that need dependable alert delivery.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReadinessCard(
            "Notifications",
            if (notificationAccess) "Allowed on this device." else "Not allowed yet. Alarm and timer features will explain and request this only when needed.",
        )
        ReadinessCard(
            "Exact alarm scheduling",
            if (exactAlarmAccess) "Available on this device." else "Not available yet. Android special access is requested only for dependable alarm/timer scheduling.",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Contextual hints", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Short tips appear near relevant features. You can dismiss individual tips or turn all ordinary hints off later.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = preferences.hintsEnabled,
                onCheckedChange = preferencesStore::setHintsEnabled,
            )
        }
    }
}

@Composable
private fun ReadyStep(
    exactAlarmAccess: Boolean,
    notificationAccess: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Ready to go",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Clock's local core is ready. Optional Android alert permissions can be completed later when you use alarms or timers.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReadinessCard("Local time tools", "Ready")
        ReadinessCard("GoreeCloud account", "Not required")
        ReadinessCard("Network connection", "Not required")
        ReadinessCard(
            "Notifications",
            if (notificationAccess) "Ready" else "Not configured — requested when needed",
        )
        ReadinessCard(
            "Exact alarm scheduling",
            if (exactAlarmAccess) "Ready" else "Not configured — requested when needed",
        )
    }
}

@Composable
private fun ReadinessCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
