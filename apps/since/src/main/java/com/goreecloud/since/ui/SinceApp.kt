package com.goreecloud.since.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SinceApp() {
    var showDevelopmentBoundary by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDevelopmentBoundary = true }) {
                Text("+")
            }
        },
    ) { innerPadding ->
        DashboardEmptyState(innerPadding)
    }

    if (showDevelopmentBoundary) {
        AlertDialog(
            onDismissRequest = { showDevelopmentBoundary = false },
            title = { Text("Tracker creation is next") },
            text = {
                Text(
                    "This Development foundation verifies the app shell and time/domain engine. " +
                        "Persistent tracker creation is not implemented yet."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDevelopmentBoundary = false }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun DashboardEmptyState(
    innerPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Since",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = "Track time since an event or start a streak.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = "No trackers yet",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
