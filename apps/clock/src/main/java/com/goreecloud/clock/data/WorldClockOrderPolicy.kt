package com.goreecloud.clock.data

import java.time.ZoneId

/**
 * Keeps saved world-clock ordering deterministic and resilient to malformed local preference data.
 */
object WorldClockOrderPolicy {
    fun normalize(zones: List<String>): List<String> {
        val result = linkedSetOf<String>()
        for (raw in zones) {
            val zone = raw.trim()
            if (zone.isEmpty()) continue
            if (runCatching { ZoneId.of(zone) }.isFailure) continue
            result += zone
        }
        return result.toList()
    }

    fun add(zones: List<String>, zoneId: String): List<String> =
        normalize(zones + zoneId)

    fun remove(zones: List<String>, zoneId: String): List<String> =
        normalize(zones).filterNot { it == zoneId }

    fun move(zones: List<String>, index: Int, offset: Int): List<String> {
        val normalized = normalize(zones)
        val destination = index + offset
        if (index !in normalized.indices || destination !in normalized.indices || offset == 0) {
            return normalized
        }

        return normalized.toMutableList().also { reordered ->
            val item = reordered.removeAt(index)
            reordered.add(destination, item)
        }
    }
}
