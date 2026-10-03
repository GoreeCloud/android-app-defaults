package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherAppLockCredentialType
import com.goreecloud.launcher.core.launcher.LauncherAppLockRepository
import com.goreecloud.launcher.core.launcher.LauncherAppLockState
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeMetrics

@Composable
internal fun LauncherAppLockSetupDialog(
    initialType: LauncherAppLockCredentialType,
    onConfigure: (LauncherAppLockCredentialType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember(initialType) { mutableStateOf(initialType) }
    var firstEntry by remember(type) { mutableStateOf("") }
    var confirmation by remember(type) { mutableStateOf("") }
    var stage by remember(type) { mutableStateOf(0) }
    var localError by remember(type) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Set up App Lock")
                Text(
                    "Protect app launches inside GoreeCloud Launcher.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2)) {
                LauncherAppLockBoundaryNotice()

                if (stage == 0) {
                    Text(
                        "Choose a credential",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppLockTypeChoice(
                            label = "PIN",
                            selected = type == LauncherAppLockCredentialType.PIN,
                            onClick = { type = LauncherAppLockCredentialType.PIN },
                            modifier = Modifier.weight(1f),
                        )
                        AppLockTypeChoice(
                            label = "Pattern",
                            selected = type == LauncherAppLockCredentialType.PATTERN,
                            onClick = { type = LauncherAppLockCredentialType.PATTERN },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AppLockCredentialEntry(
                        type = type,
                        credential = firstEntry,
                        onCredentialChange = {
                            firstEntry = it
                            localError = null
                        },
                    )
                    Text(
                        when (type) {
                            LauncherAppLockCredentialType.PIN -> "Use 4–6 digits."
                            LauncherAppLockCredentialType.PATTERN ->
                                "Use at least 4 different dots. Tap dots in sequence."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Confirm your " +
                            if (type == LauncherAppLockCredentialType.PIN) "PIN" else "pattern",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AppLockCredentialEntry(
                        type = type,
                        credential = confirmation,
                        onCredentialChange = {
                            confirmation = it
                            localError = null
                        },
                    )
                }

                localError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (stage == 0) {
                        if (LauncherAppLockRepository.normalize(type, firstEntry) == null) {
                            localError = when (type) {
                                LauncherAppLockCredentialType.PIN ->
                                    "PIN must contain 4–6 digits."
                                LauncherAppLockCredentialType.PATTERN ->
                                    "Pattern must contain 4–9 different dots."
                            }
                        } else {
                            stage = 1
                        }
                    } else if (confirmation != firstEntry) {
                        localError = "The entries do not match."
                    } else {
                        onConfigure(type, firstEntry)
                    }
                },
            ) {
                Text(if (stage == 0) "Continue" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (stage == 1) {
                        stage = 0
                        confirmation = ""
                        localError = null
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(if (stage == 1) "Back" else "Cancel")
            }
        },
    )
}

@Composable
internal fun LauncherAppLockVerifyDialog(
    title: String,
    subtitle: String,
    credentialType: LauncherAppLockCredentialType,
    errorMessage: String?,
    busy: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var credential by remember(credentialType, title) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2)) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LauncherAppLockBoundaryNotice()
                AppLockCredentialEntry(
                    type = credentialType,
                    credential = credential,
                    onCredentialChange = { credential = it },
                    enabled = !busy,
                )
                errorMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(credential) },
                enabled = !busy &&
                    LauncherAppLockRepository.normalize(credentialType, credential) != null,
            ) {
                Text(if (busy) "Checking…" else "Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun LauncherAppLockSettingsContent(
    state: LauncherAppLockState,
    apps: List<LauncherActivityInfo>,
    onSetUp: (LauncherAppLockCredentialType) -> Unit,
    onChangeCredential: () -> Unit,
    onDisable: () -> Unit,
    onUnlockApp: (LauncherActivityInfo) -> Unit,
) {
    LauncherAppLockBoundaryNotice()

    if (!state.configured) {
        Text(
            "Choose one credential for all apps you lock in Launcher.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { onSetUp(LauncherAppLockCredentialType.PIN) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Set up PIN")
            }
            FilledTonalButton(
                onClick = { onSetUp(LauncherAppLockCredentialType.PATTERN) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Set up pattern")
            }
        }
        Text(
            "After setup, long-press an app on Home or in Apps and choose Lock app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SettingsReadOnlyRow(
        "Credential",
        when (state.credentialType) {
            LauncherAppLockCredentialType.PIN -> "4–6 digit PIN"
            LauncherAppLockCredentialType.PATTERN -> "Pattern"
            null -> "Unavailable"
        },
    )
    SettingsReadOnlyRow(
        "Locked apps",
        state.lockedAppKeys.size.toString(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onChangeCredential,
            modifier = Modifier.weight(1f),
        ) {
            Text("Change")
        }
        TextButton(
            onClick = onDisable,
            modifier = Modifier.weight(1f),
        ) {
            Text("Turn off")
        }
    }

    val lockedApps = remember(apps, state.lockedAppKeys) {
        apps
            .filter { it.workspaceKey() in state.lockedAppKeys }
            .sortedBy { it.label.toString().lowercase() }
    }
    if (lockedApps.isEmpty()) {
        Text(
            "No apps are locked yet. Long-press an app on Home or in Apps and choose Lock app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            "Locked apps",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        lockedApps.forEach { app ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    GlazeMetrics.radiusMedium,
                ),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LauncherAppLockGlyph(
                        locked = true,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        app.label.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { onUnlockApp(app) }) {
                        Text("Unlock")
                    }
                }
            }
        }
    }

    Text(
        "Locked apps are challenged every time GoreeCloud Launcher tries to open them. " +
            "App shortcuts from Launcher are challenged too.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun LauncherAppLockBoundaryNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.50f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LauncherAppLockGlyph(
                locked = true,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Launcher-only protection",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    "App Lock only blocks launches started from GoreeCloud Launcher. " +
                        "It does not lock the app across Android; other launchers, notifications, " +
                        "system surfaces, links, or Settings may still open it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
internal fun LauncherAppLockGlyph(
    locked: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(22.dp)) {
        val u = size.minDimension
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(u * 0.19f, u * 0.43f),
            size = androidx.compose.ui.geometry.Size(u * 0.62f, u * 0.43f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.09f),
            style = Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = if (locked) 180f else 130f,
            useCenter = false,
            topLeft = Offset(u * 0.30f, u * 0.13f),
            size = androidx.compose.ui.geometry.Size(u * 0.40f, u * 0.50f),
            style = Stroke(stroke),
        )
        drawCircle(
            color = color,
            radius = u * 0.045f,
            center = Offset(u * 0.50f, u * 0.63f),
        )
    }
}

@Composable
private fun AppLockTypeChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusPill),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AppLockCredentialEntry(
    type: LauncherAppLockCredentialType,
    credential: String,
    onCredentialChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    when (type) {
        LauncherAppLockCredentialType.PIN -> {
            OutlinedTextField(
                value = credential,
                onValueChange = { value ->
                    onCredentialChange(value.filter(Char::isDigit).take(6))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        }
        LauncherAppLockCredentialType.PATTERN -> {
            AppLockPatternPad(
                pattern = credential,
                onPatternChange = onCredentialChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun AppLockPatternPad(
    pattern: String,
    onPatternChange: (String) -> Unit,
    enabled: Boolean,
) {
    val selected = pattern.mapNotNull { it.digitToIntOrNull() }.filter { it in 0..8 }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                    androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
                )
                .padding(12.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                if (selected.size > 1) {
                    val step = size.width / 3f
                    fun center(node: Int): Offset {
                        val column = node % 3
                        val row = node / 3
                        return Offset(
                            step * (column + 0.5f),
                            step * (row + 0.5f),
                        )
                    }
                    selected.zipWithNext().forEach { (from, to) ->
                        drawLine(
                            color = MaterialTheme.colorScheme.primary,
                            start = center(from),
                            end = center(to),
                            strokeWidth = 3.dp.toPx(),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.matchParentSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(3) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        repeat(3) { column ->
                            val node = row * 3 + column
                            val selectedIndex = selected.indexOf(node)
                            Surface(
                                onClick = {
                                    if (enabled && selectedIndex < 0 && selected.size < 9) {
                                        onPatternChange(pattern + node)
                                    }
                                },
                                enabled = enabled && selectedIndex < 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        contentDescription = "Pattern dot " + (node + 1)
                                    },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = if (selectedIndex >= 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    2.dp,
                                    if (selectedIndex >= 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (selectedIndex >= 0) (selectedIndex + 1).toString() else "",
                                        color = if (selectedIndex >= 0) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = {
                    if (pattern.isNotEmpty()) {
                        onPatternChange(pattern.dropLast(1))
                    }
                },
                enabled = enabled && pattern.isNotEmpty(),
            ) {
                Text("Undo")
            }
            TextButton(
                onClick = { onPatternChange("") },
                enabled = enabled && pattern.isNotEmpty(),
            ) {
                Text("Clear")
            }
        }
    }
}
