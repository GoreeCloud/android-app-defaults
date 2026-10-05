package com.goreecloud.launcher.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryFilter

internal const val LAUNCHER_DRAWER_DISCOVERY_FILTER_MIN_TARGET_DP = 48

@Composable
internal fun LauncherDrawerDiscoveryFiltersRow(
    selectedFilter: LauncherDrawerDiscoveryFilter,
    chooseFilter: (LauncherDrawerDiscoveryFilter) -> Unit,
) {
    Row {
        LauncherDrawerDiscoveryFilter.entries.forEach { filter ->
            Surface(onClick = { chooseFilter(filter) }) {
                Text(filter.displayName)
            }
        }
    }
}
