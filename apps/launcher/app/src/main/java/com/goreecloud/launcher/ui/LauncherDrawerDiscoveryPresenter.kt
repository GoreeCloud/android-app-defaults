package com.goreecloud.launcher.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryFilter

internal const val LAUNCHER_DRAWER_DISCOVERY_FILTER_MIN_TARGET_DP = 48

@Composable
internal fun LauncherDrawerDiscoveryFiltersRow(
    selectedFilter: LauncherDrawerDiscoveryFilter,
    pinnedAvailable: Boolean,
    chooseFilter: (LauncherDrawerDiscoveryFilter) -> Unit,
) {
    val filters = LauncherDrawerDiscoveryFilter.entries.filter {
        it != LauncherDrawerDiscoveryFilter.PINNED || pinnedAvailable
    }
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        filters.forEach { filter ->
            TextButton(
                onClick = { chooseFilter(filter) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (selectedFilter == filter) "• " + filter.displayName else filter.displayName)
            }
        }
    }
}
