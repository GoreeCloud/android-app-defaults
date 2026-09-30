package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherDrawerSpacing
import java.text.Normalizer
import java.util.Locale

/**
 * Treat apps and folders as peers in the app drawer. Sorting must remain stable regardless
 * of a provider's item-list order or the work profile's separate application enumeration.
 * Canonically equivalent Unicode labels must sort together, like the installed-app inventory.
 * Only presentation order changes: folder membership and persisted Home positions are untouched.
 */
internal enum class LauncherDrawerSortOrder(
    val displayName: String,
) {
    ALPHABETICAL("A–Z"),
    REVERSE_ALPHABETICAL("Z–A"),
    MOST_RECENT("Most Recent"),
    MOST_FREQUENT("Most Frequent"),
}

internal object LauncherDrawerSortingPolicy {
    fun <T> order(
        entries: List<T>,
        label: (T) -> String,
        key: (T) -> String,
        sortOrder: LauncherDrawerSortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
        recentKeys: List<String> = emptyList(),
        launchCounts: Map<String, Long> = emptyMap(),
        usageKey: (T) -> String? = { null },
    ): List<T> {
        val recentRank = recentKeys
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .withIndex()
            .associate { indexed -> indexed.value to indexed.index }

        fun normalizedLabel(entry: T): String =
            Normalizer.normalize(label(entry), Normalizer.Form.NFC).lowercase(Locale.ROOT)

        fun labelThenKey(left: T, right: T, reverse: Boolean = false): Int {
            val labelOrder = normalizedLabel(left).compareTo(normalizedLabel(right))
            val directed = if (reverse) -labelOrder else labelOrder
            return if (directed != 0) directed else key(left).compareTo(key(right))
        }

        return entries.sortedWith { left, right ->
            when (sortOrder) {
                LauncherDrawerSortOrder.ALPHABETICAL ->
                    labelThenKey(left, right)

                LauncherDrawerSortOrder.REVERSE_ALPHABETICAL ->
                    labelThenKey(left, right, reverse = true)

                LauncherDrawerSortOrder.MOST_RECENT -> {
                    val leftRank = usageKey(left)?.let(recentRank::get)
                    val rightRank = usageKey(right)?.let(recentRank::get)
                    when {
                        leftRank != null && rightRank != null && leftRank != rightRank ->
                            leftRank.compareTo(rightRank)
                        leftRank != null && rightRank == null -> -1
                        leftRank == null && rightRank != null -> 1
                        else -> labelThenKey(left, right)
                    }
                }

                LauncherDrawerSortOrder.MOST_FREQUENT -> {
                    val leftCount = usageKey(left)?.let(launchCounts::get)?.takeIf { it > 0L }
                    val rightCount = usageKey(right)?.let(launchCounts::get)?.takeIf { it > 0L }
                    when {
                        leftCount != null && rightCount != null && leftCount != rightCount ->
                            rightCount.compareTo(leftCount)
                        leftCount != null && rightCount == null -> -1
                        leftCount == null && rightCount != null -> 1
                        else -> labelThenKey(left, right)
                    }
                }
            }
        }
    }
}

/** Fixed cell geometry for a uniform app-drawer grid. */
internal data class LauncherDrawerGridGeometry(
    val tileHeightDp: Int,
    val iconSlotHeightDp: Int,
    val labelSlotHeightDp: Int,
)

internal object LauncherDrawerGridPolicy {
    const val ICON_SLOT_HEIGHT_DP = 60
    const val GRID_LABEL_SLOT_HEIGHT_DP = 34
    const val COMPACT_LABEL_SLOT_HEIGHT_DP = 20

    fun geometry(
        compact: Boolean,
        spacing: LauncherDrawerSpacing,
    ): LauncherDrawerGridGeometry {
        val tileHeight = if (compact) {
            when (spacing) {
                LauncherDrawerSpacing.TIGHT -> 88
                LauncherDrawerSpacing.STANDARD -> 94
                LauncherDrawerSpacing.RELAXED -> 100
            }
        } else {
            when (spacing) {
                LauncherDrawerSpacing.TIGHT -> 104
                LauncherDrawerSpacing.STANDARD -> 110
                LauncherDrawerSpacing.RELAXED -> 116
            }
        }
        return LauncherDrawerGridGeometry(
            tileHeightDp = tileHeight,
            iconSlotHeightDp = ICON_SLOT_HEIGHT_DP,
            labelSlotHeightDp = if (compact) COMPACT_LABEL_SLOT_HEIGHT_DP else GRID_LABEL_SLOT_HEIGHT_DP,
        )
    }
}

/**
 * Drawer page indicators keep a restrained visual dot while preserving the Glaze interaction
 * floor. The visual size is never used as the touch target.
 */
internal object LauncherDrawerPageIndicatorPolicy {
    const val TOUCH_TARGET_DP = 48
    const val SELECTED_VISUAL_DP = 8
    const val IDLE_VISUAL_DP = 6

    fun visualSizeDp(selected: Boolean): Int =
        if (selected) SELECTED_VISUAL_DP else IDLE_VISUAL_DP
}
