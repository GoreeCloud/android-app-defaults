package com.goreecloud.launcher.core.launcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device-local API key storage never shares ordinary prefs, backups, logs, or Search history. */
@RunWith(AndroidJUnit4::class)
class LauncherUserApiSourcesRuntimeTest {
    @Test
    fun savedSecretIsEncryptedOffBackupAndDisconnectRemovesEntry() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = LauncherUserApiSourceRepository(context)
        val id = "api.custom." + UUID.randomUUID().toString()
        val secret = "fake-test-key-" + UUID.randomUUID().toString()
        val store = File(context.noBackupFilesDir, "launcher_search_user_apis.v1")

        try {
            repository.refresh()
            repository.save(
                id,
                LauncherUserApiSourceInput(
                    title = "Fixture API",
                    kind = LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE,
                    model = "fixture-model",
                    endpoint = "https://test-api.example.org/v1/chat/completions",
                    newApiKey = secret,
                    enabled = false,
                ),
            )

            assertTrue(repository.sources.value.any {
                it.id == id && it.configured && !it.enabled
            })
            assertTrue(store.exists())
            assertTrue(store.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath))
            assertFalse(String(store.readBytes(), Charsets.ISO_8859_1).contains(secret))

            repository.refresh()
            assertTrue(repository.sources.value.any { it.id == id && it.configured })

            repository.setEnabled(id, true)
            assertTrue(repository.sources.value.any { it.id == id && it.enabled })
            repository.setEnabled(id, false)
            assertFalse(repository.sources.value.first { it.id == id }.enabled)
        } finally {
            repository.remove(id)
        }
        assertFalse(repository.sources.value.any { it.id == id })
    }
}
