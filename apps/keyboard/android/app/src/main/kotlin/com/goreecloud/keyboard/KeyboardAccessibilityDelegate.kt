package com.goreecloud.keyboard

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import kotlin.math.roundToInt

internal data class KeyboardAccessibilityTarget(
    val id: Int,
    val bounds: android.graphics.RectF,
    val label: String,
    val selected: Boolean = false,
)

/**
 * Exposes the custom-drawn KeyboardView controls as real virtual accessibility nodes.
 *
 * This delegate deliberately consumes only the already-rendered interaction geometry and
 * visible labels supplied by KeyboardView. It does not inspect editor contents, clipboard
 * state, suggestion history, network data, or any hidden input state.
 *
 * Long-press alternates remain sourced exclusively from the device-local KeyAlternates
 * catalog. Eligible keys expose both ACTION_LONG_CLICK as a discovery hint and bounded custom
 * actions for exact alternate activation. Alternate activation routes through the same
 * KeyboardView.Listener text path used by the rendered keyboard; this delegate does not gain
 * editor-observation authority. When the rendered emoji-search keyboard is active, alternates
 * stay disabled just as they are for touch input so accessibility cannot bypass that mode boundary.
 */
internal class KeyboardAccessibilityDelegate(
    private val keyboardView: KeyboardView,
) : ExploreByTouchHelper(keyboardView) {

    override fun getVirtualViewAt(x: Float, y: Float): Int =
        keyboardView.accessibilityTargets()
            .lastOrNull { it.bounds.contains(x, y) }
            ?.id
            ?: INVALID_ID

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        keyboardView.accessibilityTargets().forEach { virtualViewIds += it.id }
    }

    override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        node: AccessibilityNodeInfoCompat,
    ) {
        val target = keyboardView.accessibilityTarget(virtualViewId)
        if (target == null) {
            node.contentDescription = "Unavailable keyboard control"
            node.className = "android.widget.Button"
            node.isEnabled = false
            node.setBoundsInParent(Rect(0, 0, 1, 1))
            return
        }

        node.contentDescription = target.label
        node.className = "android.widget.Button"
        node.isEnabled = true
        node.isFocusable = true
        node.isClickable = true
        node.isSelected = target.selected
        node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)

        val alternates = alternatesFor(target)
        if (alternates.isNotEmpty()) {
            node.isLongClickable = true
            node.hintText = "Alternate characters available"
            node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK)
            alternates.forEachIndexed { index, value ->
                node.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        alternateActionIds[index],
                        "Insert $value",
                    )
                )
            }
        }

        node.setBoundsInParent(target.bounds.toAccessibilityRect())
    }

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?,
    ): Boolean {
        val target = keyboardView.accessibilityTarget(virtualViewId) ?: return false
        val alternates = alternatesFor(target)

        if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
            if (alternates.isEmpty()) return false
            keyboardView.announceForAccessibility(
                "Alternate characters: ${alternates.joinToString(separator = ", ")}",
            )
            sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_ANNOUNCEMENT)
            return true
        }

        val alternateIndex = alternateActionIds.indexOf(action)
        if (alternateIndex >= 0) {
            val value = alternates.getOrNull(alternateIndex) ?: return false
            keyboardView.listener?.onText(value)
            keyboardView.performClick()
            keyboardView.announceForAccessibility("Inserted alternate character $value")
            sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
            return true
        }

        if (action != AccessibilityNodeInfo.ACTION_CLICK) return false
        if (!keyboardView.performAccessibilityTarget(virtualViewId)) return false
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
        return true
    }

    fun invalidateVirtualRoot() {
        invalidateRoot()
    }

    private fun alternatesFor(target: KeyboardAccessibilityTarget): List<String> {
        if (emojiSearchKeyboardIsActive()) return emptyList()
        return KeyAlternates.forKey(target.label).take(alternateActionIds.size)
    }

    private fun emojiSearchKeyboardIsActive(): Boolean =
        keyboardView.accessibilityTargets().any { target ->
            target.label == "Clear emoji search" || target.label == "Close emoji search"
        }

    private fun android.graphics.RectF.toAccessibilityRect(): Rect {
        val left = left.roundToInt()
        val top = top.roundToInt()
        val right = right.roundToInt().coerceAtLeast(left + 1)
        val bottom = bottom.roundToInt().coerceAtLeast(top + 1)
        return Rect(left, top, right, bottom)
    }

    private companion object {
        val alternateActionIds = intArrayOf(
            R.id.accessibility_alternate_0,
            R.id.accessibility_alternate_1,
            R.id.accessibility_alternate_2,
            R.id.accessibility_alternate_3,
            R.id.accessibility_alternate_4,
            R.id.accessibility_alternate_5,
            R.id.accessibility_alternate_6,
        )
    }
}
