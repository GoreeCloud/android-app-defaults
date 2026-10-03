package com.goreecloud.launcher.core.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.util.concurrent.atomic.AtomicLong
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

class LauncherAppLockRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun pinConfigurationNeverExposesRawCredentialAndLocksProfileQualifiedKey() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("app-lock-pin.preferences_pb") },
        )
        val repository = LauncherAppLockRepository(store)

        try {
            assertTrue(repository.configure(LauncherAppLockCredentialType.PIN, "4826"))
            assertTrue(repository.setAppLocked("17:com.example/.MainActivity", true))

            val state = repository.state.first()
            assertTrue(state.configured)
            assertEquals(LauncherAppLockCredentialType.PIN, state.credentialType)
            assertTrue(state.isLocked("17:com.example/.MainActivity"))
            assertEquals(
                LauncherAppLockVerificationResult.Success,
                repository.verify(LauncherAppLockCredentialType.PIN, "4826"),
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun rejectsInvalidPinAndPatternShapes() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("app-lock-invalid.preferences_pb") },
        )
        val repository = LauncherAppLockRepository(store)

        try {
            assertFalse(repository.configure(LauncherAppLockCredentialType.PIN, "123"))
            assertFalse(repository.configure(LauncherAppLockCredentialType.PIN, "1234567"))
            assertFalse(repository.configure(LauncherAppLockCredentialType.PATTERN, "012"))
            assertFalse(repository.configure(LauncherAppLockCredentialType.PATTERN, "0120"))
            assertTrue(repository.configure(LauncherAppLockCredentialType.PATTERN, "01478"))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun fiveFailuresTriggerPersistedCooldownThenSuccessfulVerificationRecovers() = runBlocking {
        val now = AtomicLong(10_000L)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("app-lock-cooldown.preferences_pb") },
        )
        val repository = LauncherAppLockRepository(
            dataStore = store,
            nowMillis = now::get,
        )

        try {
            assertTrue(repository.configure(LauncherAppLockCredentialType.PIN, "4826"))
            repeat(4) { index ->
                assertEquals(
                    LauncherAppLockVerificationResult.Invalid(4 - index),
                    repository.verify(LauncherAppLockCredentialType.PIN, "1111"),
                )
            }
            val cooldown = repository.verify(LauncherAppLockCredentialType.PIN, "1111")
            assertTrue(cooldown is LauncherAppLockVerificationResult.Cooldown)
            assertTrue(
                repository.verify(
                    LauncherAppLockCredentialType.PIN,
                    "4826",
                ) is LauncherAppLockVerificationResult.Cooldown,
            )

            now.addAndGet(31_000L)
            assertEquals(
                LauncherAppLockVerificationResult.Success,
                repository.verify(LauncherAppLockCredentialType.PIN, "4826"),
            )
            assertEquals(0, repository.state.first().failedAttempts)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun clearingCredentialAlsoClearsLockedApps() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("app-lock-clear.preferences_pb") },
        )
        val repository = LauncherAppLockRepository(store)

        try {
            repository.configure(LauncherAppLockCredentialType.PATTERN, "01478")
            repository.setAppLocked("9:com.example/.Main", true)
            repository.clearCredential()

            val state = repository.state.first()
            assertFalse(state.configured)
            assertTrue(state.lockedAppKeys.isEmpty())
            assertEquals(
                LauncherAppLockVerificationResult.NotConfigured,
                repository.verify(LauncherAppLockCredentialType.PATTERN, "01478"),
            )
        } finally {
            scope.cancel()
        }
    }
}
