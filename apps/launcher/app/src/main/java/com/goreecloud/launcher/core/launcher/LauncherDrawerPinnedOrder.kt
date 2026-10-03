package com.goreecloud.launcher.core.launcher

/**
 * Device-local App Drawer presentation metadata.
 *
 * Pin membership/order and hidden discovery identities use exact profile-qualified workspace keys.
 * This sidecar state remains intentionally outside portable preference v1.
 */
data class LauncherDrawerPinnedState(
    val keys: Set<String>,
    val order: List<String>,
    val hiddenKeys: Set<String> = emptySet(),
)

internal object LauncherDrawerPinnedOrder {
    fun encode(keys: List<String>): String =
        keys.asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(separator = "") { key -> "${key.length}:$key" }

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var cursor = 0
        while (cursor < raw.length) {
            val colon = raw.indexOf(':', cursor)
            if (colon <= cursor) return emptyList()
            val length = raw.substring(cursor, colon).toIntOrNull() ?: return emptyList()
            if (length <= 0 || length > 16_384) return emptyList()
            val start = colon + 1
            val end = start + length
            if (end > raw.length) return emptyList()
            val key = raw.substring(start, end)
            if (key.isBlank()) return emptyList()
            if (key !in result) result += key
            cursor = end
        }
        return result
    }

    fun reconcile(
        order: List<String>,
        pinnedKeys: Set<String>,
    ): List<String> {
        if (pinnedKeys.isEmpty()) return emptyList()
        val result = order.filter { it in pinnedKeys }.distinct().toMutableList()
        val seen = result.toHashSet()
        result += pinnedKeys.asSequence().filterNot(seen::contains).sorted()
        return result
    }

    fun move(
        order: List<String>,
        pinnedKeys: Set<String>,
        appKey: String,
        delta: Int,
    ): List<String> {
        val reconciled = reconcile(order, pinnedKeys).toMutableList()
        val from = reconciled.indexOf(appKey)
        if (from < 0) return reconciled
        val to = (from + delta).coerceIn(0, reconciled.lastIndex)
        if (from == to) return reconciled
        val value = reconciled.removeAt(from)
        reconciled.add(to, value)
        return reconciled
    }
}

/**
 * Device-local custom ordering for App Drawer application identities.
 *
 * Unlike Home folders or Room placement, this is presentation metadata only. Keys remain the exact
 * profile-qualified Launcher workspace identities already used by the drawer and Hidden Apps.
 */
internal object LauncherDrawerCustomOrder {
    fun encode(keys: List<String>): String = LauncherDrawerPinnedOrder.encode(keys)

    fun decode(raw: String?): List<String> = LauncherDrawerPinnedOrder.decode(raw)

    fun reconcile(
        order: List<String>,
        availableKeys: Set<String>,
    ): List<String> {
        if (availableKeys.isEmpty()) return emptyList()
        val result = order.filter { it in availableKeys }.distinct().toMutableList()
        val seen = result.toHashSet()
        result += availableKeys.asSequence().filterNot(seen::contains).sorted()
        return result
    }

    /**
     * Move one app relative only to peers in its exact Android profile while preserving the slots
     * occupied by every other profile. This keeps User/Work/private identities independent even
     * when their keys share one device-local order record.
     */
    fun moveWithinProfile(
        order: List<String>,
        availableKeys: Set<String>,
        profileKeys: Set<String>,
        appKey: String,
        delta: Int,
    ): List<String> {
        val reconciled = reconcile(order, availableKeys)
        if (delta == 0 || appKey !in profileKeys) return reconciled

        val profileOrder = reconciled.filter { it in profileKeys }.toMutableList()
        val from = profileOrder.indexOf(appKey)
        if (from < 0) return reconciled
        val to = (from + delta).coerceIn(0, profileOrder.lastIndex)
        if (from == to) return reconciled

        val value = profileOrder.removeAt(from)
        profileOrder.add(to, value)
        return replaceProfileOrder(
            order = reconciled,
            availableKeys = availableKeys,
            profileKeys = profileKeys,
            replacementProfileOrder = profileOrder,
        )
    }

    fun replaceProfileOrder(
        order: List<String>,
        availableKeys: Set<String>,
        profileKeys: Set<String>,
        replacementProfileOrder: List<String>,
    ): List<String> {
        val reconciled = reconcile(order, availableKeys)
        val validProfileKeys = profileKeys.intersect(availableKeys)
        if (validProfileKeys.isEmpty()) return reconciled

        val replacement = replacementProfileOrder
            .asSequence()
            .filter { it in validProfileKeys }
            .distinct()
            .toMutableList()
        val seen = replacement.toHashSet()
        replacement += reconciled.asSequence()
            .filter { it in validProfileKeys && it !in seen }

        val iterator = replacement.iterator()
        return reconciled.map { key ->
            if (key in validProfileKeys) iterator.next() else key
        }
    }
}

