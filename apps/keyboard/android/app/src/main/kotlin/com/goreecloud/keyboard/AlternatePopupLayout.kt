package com.goreecloud.keyboard

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class AlternatePopupSourceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(right > left) { "source bounds must have positive width" }
        require(bottom > top) { "source bounds must have positive height" }
    }

    val centerX: Float
        get() = (left + right) / 2f
}

data class AlternatePopupCellBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class AlternatePopupLayoutResult(
    val left: Float,
    val top: Float,
    val columns: Int,
    val rows: Int,
    val contentWidth: Float,
    val contentHeight: Float,
    val placedAboveSource: Boolean,
    val itemCount: Int,
    val cellSize: Float,
    val gap: Float,
) {
    fun itemBounds(index: Int): AlternatePopupCellBounds {
        require(index in 0 until itemCount) { "alternate item index is outside the layout" }
        val row = index / columns
        val column = index % columns
        val itemLeft = left + column * (cellSize + gap)
        val itemTop = top + row * (cellSize + gap)
        return AlternatePopupCellBounds(
            left = itemLeft,
            top = itemTop,
            right = itemLeft + cellSize,
            bottom = itemTop + cellSize,
        )
    }

    /** Returns the exact alternate item under a pointer, excluding inter-cell gaps. */
    fun hitTest(x: Float, y: Float): Int? {
        if (!x.isFinite() || !y.isFinite()) return null
        if (x < left || y < top || x >= left + contentWidth || y >= top + contentHeight) return null

        val stride = cellSize + gap
        val column = floor((x - left) / stride).toInt()
        val row = floor((y - top) / stride).toInt()
        if (column !in 0 until columns || row !in 0 until rows) return null

        val index = row * columns + column
        if (index !in 0 until itemCount) return null
        val bounds = itemBounds(index)
        return index.takeIf {
            x >= bounds.left && x < bounds.right && y >= bounds.top && y < bounds.bottom
        }
    }
}

/**
 * Pure viewport-bounded geometry policy for native long-press key alternates.
 *
 * The policy uses only layout geometry. It performs no text inspection, editor-context read,
 * learning, persistence, or network work.
 */
object AlternatePopupLayout {
    private const val MaxColumns = 5

    fun calculate(
        source: AlternatePopupSourceBounds,
        itemCount: Int,
        viewportWidth: Float,
        viewportHeight: Float,
        cellSize: Float,
        gap: Float,
    ): AlternatePopupLayoutResult {
        require(itemCount > 0) { "alternate item count must be positive" }
        require(viewportWidth > 0f && viewportHeight > 0f) { "viewport must be positive" }
        require(cellSize > 0f) { "cell size must be positive" }
        require(gap >= 0f) { "gap must be non-negative" }

        val availableWidth = viewportWidth - 2f * gap
        val maxColumnsForViewport = floor((availableWidth + gap) / (cellSize + gap)).toInt()
        require(maxColumnsForViewport >= 1) { "viewport cannot fit one alternate cell" }
        val columns = min(MaxColumns, min(itemCount, maxColumnsForViewport))
        val rows = ceil(itemCount / columns.toFloat()).toInt()
        val contentWidth = columns * cellSize + (columns - 1) * gap
        val contentHeight = rows * cellSize + (rows - 1) * gap
        require(contentHeight + 2f * gap <= viewportHeight) { "viewport cannot fit alternate popup height" }

        val maxLeft = max(gap, viewportWidth - gap - contentWidth)
        val left = (source.centerX - contentWidth / 2f).coerceIn(gap, maxLeft)
        val aboveTop = source.top - gap - contentHeight
        val belowTop = source.bottom + gap
        val canPlaceAbove = aboveTop >= gap
        val canPlaceBelow = belowTop + contentHeight <= viewportHeight - gap
        val top = when {
            canPlaceAbove -> aboveTop
            canPlaceBelow -> belowTop
            else -> (source.top - contentHeight / 2f).coerceIn(
                gap,
                viewportHeight - gap - contentHeight,
            )
        }

        return AlternatePopupLayoutResult(
            left = left,
            top = top,
            columns = columns,
            rows = rows,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            placedAboveSource = canPlaceAbove,
            itemCount = itemCount,
            cellSize = cellSize,
            gap = gap,
        )
    }
}
