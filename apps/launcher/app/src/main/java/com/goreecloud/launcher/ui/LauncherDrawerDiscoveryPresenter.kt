package com.goreecloud.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryFilter

internal const val LAUNCHER_DRAWER_DISCOVERY_FILTER_MIN_TARGET_DP = 48

@Composable
internal fun LauncherDrawerDiscoveryFiltersRow(
    selectedFilter: LauncherDrawerDiscoveryFilter,
    filters: List<LauncherDrawerDiscoveryFilter>,
    secondaryColor: Color,
    chooseFilter: (LauncherDrawerDiscoveryFilter) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.forEach { filter ->
            val selected = filter == selectedFilter
            Surface(
                onClick = { chooseFilter(filter) },
                modifier = Modifier
                    .heightIn(min = LAUNCHER_DRAWER_DISCOVERY_FILTER_MIN_TARGET_DP.dp)
                    .semantics {
                        contentDescription = filter.accessibilityName
                        stateDescription = if (selected) "Selected" else "Not selected"
                    },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
                } else {
                    Color.Transparent
                },
                border = if (selected) {
                    null
                } else {
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
                    )
                },
            ) {
                Text(
                    text = filter.displayName,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        secondaryColor
                    },
                    maxLines = 1,
                )
            }
        }
    }
}
