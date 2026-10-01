package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.launcherSearchHistoryStore by
    preferencesDataStore(name = "launcher_search_history")

data class LauncherSearchHistoryState(
    val enabled: Boolean = false,
    val recentQueries: List<String> = emptyList(),
)

internal object LauncherSearchHistoryPolicy {
    const val MAX_QUERIES: Int = 10
    const val MAX_QUERY_LENGTH: Int = 120

    fun normalize(rawQuery: String): String? =
        rawQuery
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_QUERY_LENGTH)
            .takeIf(String::isNotBlank)

    fun record(
        existing: List<String>,
        rawQuery: String,
    ): List<String> {
        val query = normalize(rawQuery) ?: return existing.take(MAX_QUERIES)
        return buildList {
            add(query)
            existing
                .asSequence()
                .mapNotNull(::normalize)
                .filterNot { it.equals(query, ignoreCase = true) }
                .forEach(::add)
        }.take(MAX_QUERIES)
    }
}

/**
 * Compact length-prefixed codec so arbitrary local query text round-trips without JSON or a new
 * serialization dependency. Invalid/truncated payloads fail closed to an empty history.
 */
internal object LauncherSearchHistoryCodec {
    fun encode(queries: List<String>): String = buildString {
        queries.forEach { query ->
            append(query.length)
            append(':')
            append(query)
        }
    }

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val decoded = mutableListOf<String>()
        var cursor = 0
        while (cursor < raw.length) {
            val separator = raw.indexOf(':', startIndex = cursor)
            if (separator <= cursor) return emptyList()
            val length = raw.substring(cursor, separator).toIntOrNull()
                ?: return emptyList()
            if (length < 0) return emptyList()
            val start = separator + 1
            if (length > raw.length - start) return emptyList()
            val end = start + length
            decoded += raw.substring(start, end)
            cursor = end
        }
        return decoded
    }
}

/**
 * User-controlled, device-local Universal Search history.
 *
 * Search text is never persisted while the setting is disabled. Disabling history also clears any
 * previously stored queries so the setting is a real privacy boundary rather than presentation-only
 * state. Callers should record only after an explicit search action, never on each keystroke.
 */
class LauncherSearchHistoryRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.launcherSearchHistoryStore)

    private object Keys {
        val enabled = booleanPreferencesKey("enabled_v1")
        val queries = stringPreferencesKey("recent_queries_v1")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: Flow<LauncherSearchHistoryState> = dataStore.data
        .map { values ->
            val enabled = values[Keys.enabled] ?: false
            LauncherSearchHistoryState(
                enabled = enabled,
                recentQueries = if (enabled) {
                    LauncherSearchHistoryCodec.decode(values[Keys.queries])
                        .mapNotNull(LauncherSearchHistoryPolicy::normalize)
                        .take(LauncherSearchHistoryPolicy.MAX_QUERIES)
                } else {
                    emptyList()
                },
            )
        }
        .distinctUntilChanged()

    fun setEnabled(enabled: Boolean): Job = scope.launch {
        dataStore.edit { values ->
            values[Keys.enabled] = enabled
            if (!enabled) values.remove(Keys.queries)
        }
    }

    fun recordQuery(rawQuery: String): Job = scope.launch {
        dataStore.edit { values ->
            if (values[Keys.enabled] != true) return@edit
            val updated = LauncherSearchHistoryPolicy.record(
                existing = LauncherSearchHistoryCodec.decode(values[Keys.queries]),
                rawQuery = rawQuery,
            )
            if (updated.isEmpty()) {
                values.remove(Keys.queries)
            } else {
                values[Keys.queries] = LauncherSearchHistoryCodec.encode(updated)
            }
        }
    }

    fun clear(): Job = scope.launch {
        dataStore.edit { values -> values.remove(Keys.queries) }
    }
}
