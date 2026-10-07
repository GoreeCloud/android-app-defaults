package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherConnectedSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherUserApiKind
import com.goreecloud.launcher.core.launcher.LauncherUserApiSourceInput
import com.goreecloud.launcher.core.launcher.LauncherUserApiSourcePolicy
import com.goreecloud.launcher.core.launcher.LauncherUserApiSourceRepository
import com.goreecloud.launcher.core.launcher.LauncherUserApiSourceSummary
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Separately managed, user-owned API sources. The source controls never display existing API keys.
 * Provider calls happen only through the explicit on-demand Search action below.
 */
@Composable
internal fun LauncherUserApiSourcesManager(
    repository: LauncherUserApiSourceRepository,
    sources: List<LauncherUserApiSourceSummary>,
    apps: List<LauncherActivityInfo>,
) {
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<LauncherUserApiSourceSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val customCount = sources.count { it.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Text(
                "Your AI & custom APIs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Connect an API key and model, then tap Ask inside Universal Search to read its " +
                    "answer without opening another app. API calls can incur provider charges.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Keys are encrypted on this device and excluded from backup. Queries go only " +
                    "to the provider you explicitly tap; Launcher does not send every keystroke.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LauncherUserApiSourceIcon(source = source, apps = apps)
                    Column(Modifier.weight(1f)) {
                        Text(
                            source.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            when {
                                !source.configured -> "Not connected"
                                source.enabled -> "Connected · Ask on demand"
                                else -> "Connected · Disabled"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { error = null; editor = source },
                    ) {
                        Text(if (source.configured) "Manage" else "Connect")
                    }
                    Switch(
                        checked = source.configured && source.enabled,
                        enabled = source.configured,
                        modifier = Modifier.semantics {
                            contentDescription = "Enable " + source.title + " API"
                        },
                        onCheckedChange = { next ->
                            scope.launch {
                                try {
                                    repository.setEnabled(source.id, next)
                                } catch (_: Exception) {
                                    error = "Could not update API source. Check secure local storage."
                                }
                            }
                        },
                    )
                }
            }
            TextButton(
                onClick = {
                    error = null
                    editor = LauncherUserApiSourceSummary(
                        id = "api.custom." + UUID.randomUUID().toString(),
                        title = "Custom API",
                        kind = LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE,
                        model = "",
                        endpoint = "",
                        enabled = false,
                        configured = false,
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = customCount < LauncherUserApiSourcePolicy.MAX_CUSTOM_SOURCES,
            ) {
                Text("Add custom API")
            }
            if (error != null) {
                Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    editor?.let { current ->
        LauncherUserApiSourceEditor(
            source = current,
            onDismiss = { editor = null },
            onSave = { input ->
                scope.launch {
                    try {
                        repository.save(current.id, input)
                        error = null
                        editor = null
                    } catch (_: Exception) {
                        error = "Could not save API source. Check credentials, model and secure storage."
                    }
                }
            },
            onDisconnect = {
                scope.launch {
                    try {
                        repository.remove(current.id)
                        editor = null
                    } catch (_: Exception) {
                        error = "Could not disconnect API source."
                    }
                }
            },
        )
    }
}


/** Prefer the official installed provider app artwork; otherwise show a neutral API mark. */
@Composable
private fun LauncherUserApiSourceIcon(
    source: LauncherUserApiSourceSummary,
    apps: List<LauncherActivityInfo>,
) {
    val packages = remember(source.kind) {
        val handoffProviderId = when (source.kind) {
            LauncherUserApiKind.OPENAI ->
                LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID
            LauncherUserApiKind.ANTHROPIC ->
                LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID
            LauncherUserApiKind.GEMINI ->
                LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID
            LauncherUserApiKind.PERPLEXITY ->
                LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID
            LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE -> ""
        }
        LauncherConnectedSearchProviderRegistry.iconPackageNamesFor(handoffProviderId)
    }
    val matched = remember(apps, packages) {
        apps.firstOrNull { app -> app.componentName.packageName in packages }
    }
    val bitmap = matched?.let { rememberLauncherAppIcon(it) }
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(28.dp).launcherIconMask(),
                )
            } else {
                Text(
                    "API",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LauncherUserApiSourceEditor(
    source: LauncherUserApiSourceSummary,
    onDismiss: () -> Unit,
    onSave: (LauncherUserApiSourceInput) -> Unit,
    onDisconnect: () -> Unit,
) {
    var title by remember(source.id) { mutableStateOf(source.title) }
    var model by remember(source.id) { mutableStateOf(source.model) }
    var endpoint by remember(source.id) { mutableStateOf(source.endpoint) }
    var apiKey by remember(source.id) { mutableStateOf("") }
    var error by remember(source.id) { mutableStateOf<String?>(null) }
    var confirmDisconnect by remember(source.id) { mutableStateOf(false) }
    val custom = source.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (source.configured) "Manage API source" else "Connect API source") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (custom) {
                        "Custom APIs currently support the OpenAI-compatible chat-completions " +
                            "JSON format over public HTTPS. The endpoint receives your key and " +
                            "only questions you explicitly submit."
                    } else {
                        "Connect your " + source.kind.title + " account's own API key and model. " +
                            "An API subscription or billing account may be required."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (custom) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(50) },
                        label = { Text("Source name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it.take(300) },
                        label = { Text("HTTPS chat-completions endpoint") },
                        placeholder = { Text("https://api.example.com/v1/chat/completions") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it.take(120) },
                    label = { Text("Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.take(4096) },
                    label = { Text(if (source.configured) "Replace API key (optional)" else "API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (source.configured) {
                    Text(
                        "Leave the API key blank to keep the existing encrypted key.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { confirmDisconnect = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Disconnect and delete key") }
                }
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = LauncherUserApiSourceInput(
                        title = title.trim(),
                        kind = source.kind,
                        model = model.trim(),
                        endpoint = endpoint.trim(),
                        newApiKey = apiKey,
                        enabled = source.enabled || !source.configured,
                    )
                    error = LauncherUserApiSourcePolicy.validate(source.id, input)
                        ?: if (!source.configured && apiKey.isBlank()) {
                            "Enter an API key before connecting"
                        } else {
                            null
                        }
                    if (error == null) onSave(input)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Save connection") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Cancel") }
        },
    )

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect " + source.title + "?") },
            text = {
                Text("This deletes the saved API key on this device. Reconnecting requires a new key.")
            },
            confirmButton = {
                TextButton(onClick = { confirmDisconnect = false; onDisconnect() }) {
                    Text("Delete key")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text("Keep connection") }
            },
        )
    }
}

/** A single request from an explicit tap, never the live per-keystroke search pipeline. */
@Composable
internal fun LauncherUserApiAskPanel(
    repository: LauncherUserApiSourceRepository,
    sources: List<LauncherUserApiSourceSummary>,
    query: String,
) {
    val available = sources.filter { it.configured && it.enabled }
    var requested by remember { mutableStateOf<Triple<String, String, Long>?>(null) }
    var requestNumber by remember { mutableStateOf(0L) }
    var answer by remember { mutableStateOf<Pair<String, String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        requested = null
        answer = null
        error = null
    }
    LaunchedEffect(requested) {
        val request = requested ?: return@LaunchedEffect
        if (request.second != query) return@LaunchedEffect
        busy = true
        error = null
        answer = null
        try {
            val text = repository.ask(request.first, request.second)
            if (requested == request) {
                val name = available.firstOrNull { it.id == request.first }?.title ?: "API"
                answer = name to text
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (requested == request) {
                error = "No answer received. Check the provider key, model, quota and connection."
            }
        } finally {
            busy = false
        }
    }
    if (available.isEmpty() || query.isBlank()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Ask an API here",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                available.forEach { source ->
                    TextButton(
                        onClick = {
                            requestNumber += 1L
                            requested = Triple(source.id, query, requestNumber)
                            answer = null
                            error = null
                        },
                        enabled = !busy && query.length <= LauncherUserApiSourcePolicy.MAX_QUERY_CHARS,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Ask " + source.title)
                    }
                }
            }
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Waiting for selected API…", style = MaterialTheme.typography.bodySmall)
                }
            }
            answer?.let { (provider, body) ->
                Text(
                    "$provider answer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "Only the tapped provider receives this question. Provider usage charges and " +
                    "retention rules apply. Answers are not saved in Launcher.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
