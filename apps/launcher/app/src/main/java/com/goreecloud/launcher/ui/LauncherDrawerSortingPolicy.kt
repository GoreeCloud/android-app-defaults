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
internal enum class LauncherDrawerSortOrder {
    ALPHABETICAL,
    REVERSE_ALPHABETICAL,
    MOST_RECENT,
    MOST_FREQUENT,
}

internal object LauncherDrawerSortingPolicy {
    fun <T> order(
        entries: List<T>,
        label: (T) -> String,
        key: (T) -> String,
        sortOrder: LauncherDrawerSortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
        recentRank: (T) -> Int? = { null },
        frequency: (T) -> Long = { 0L },
    ): List<T> {
        fun normalizedLabel(entry: T): String =
            Normalizer.normalize(label(entry), Normalizer.Form.NFC).lowercase(Locale.ROOT)

        fun alphabetical(left: T, right: T, reversed: Boolean = false): Int {
            val labelOrder = normalizedLabel(left).compareTo(normalizedLabel(right))
            val directed = if (reversed) -labelOrder else labelOrder
            return if (directed != 0) directed else key(left).compareTo(key(right))
        }

        return entries.sortedWith { left, right ->
            when (sortOrder) {
                LauncherDrawerSortOrder.ALPHABETICAL ->
                    alphabetical(left, right)

                LauncherDrawerSortOrder.REVERSE_ALPHABETICAL ->
                    alphabetical(left, right, reversed = true)

                LauncherDrawerSortOrder.MOST_RECENT -> {
                    val leftRank = recentRank(left)
                    val rightRank = recentRank(right)
                    when {
                        leftRank != null && rightRank != null && leftRank != rightRank ->
                            leftRank.compareTo(rightRank)
                        leftRank != null && rightRank == null -> -1
                        leftRank == null && rightRank != null -> 1
                        else -> alphabetical(left, right)
                    }
                }

                LauncherDrawerSortOrder.MOST_FREQUENT -> {
                    val leftCount = frequency(left)
                    val rightCount = frequency(right)
                    when {
                        leftCount != rightCount -> rightCount.compareTo(leftCount)
                        else -> alphabetical(left, right)
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
