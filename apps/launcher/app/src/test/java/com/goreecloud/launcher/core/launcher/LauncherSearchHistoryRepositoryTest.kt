package com.goreecloud.launcher.core.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LauncherSearchHistoryRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun disabledHistoryDoesNotPersistQueriesAndDisableClearsExistingData() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("search-history.preferences_pb") },
        )
        val repository = LauncherSearchHistoryRepository(store)

        try {
            repository.recordQuery("private query").join()
            assertFalse(repository.state.first().enabled)
            assertEquals(emptyList<String>(), repository.state.first().recentQueries)

            repository.setEnabled(true).join()
            repository.recordQuery("private query").join()
            assertEquals(listOf("private query"), repository.state.first().recentQueries)

            repository.setEnabled(false).join()
            val disabled = repository.state.first()
            assertFalse(disabled.enabled)
            assertEquals(emptyList<String>(), disabled.recentQueries)

            repository.setEnabled(true).join()
            assertEquals(emptyList<String>(), repository.state.first().recentQueries)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun recordNormalizesDeduplicatesAndBoundsNewestFirst() {
        var history = emptyList<String>()
        repeat(12) { index ->
            history = LauncherSearchHistoryPolicy.record(history, "  Query   $index  ")
        }

        assertEquals(LauncherSearchHistoryPolicy.MAX_QUERIES, history.size)
        assertEquals("Query 11", history.first())
        assertEquals("Query 2", history.last())

        val deduplicated = LauncherSearchHistoryPolicy.record(history, "query 7")
        assertEquals("query 7", deduplicated.first())
        assertEquals(1, deduplicated.count { it.equals("query 7", ignoreCase = true) })
    }

    @Test
    fun codecRoundTripsArbitraryTextAndRejectsTruncatedPayload() {
        val queries = listOf("weather tomorrow", "files: Q4/report", "こんにちは 世界")
        val encoded = LauncherSearchHistoryCodec.encode(queries)

        assertEquals(queries, LauncherSearchHistoryCodec.decode(encoded))
        assertEquals(emptyList<String>(), LauncherSearchHistoryCodec.decode(encoded.dropLast(1)))
    }

    @Test
    fun normalizationCapsSensitivePayloadLengthWithoutDroppingUsefulText() {
        val normalized = LauncherSearchHistoryPolicy.normalize("   " + "a".repeat(200) + "   ")

        assertTrue(normalized != null)
        assertEquals(LauncherSearchHistoryPolicy.MAX_QUERY_LENGTH, normalized!!.length)
    }
}
