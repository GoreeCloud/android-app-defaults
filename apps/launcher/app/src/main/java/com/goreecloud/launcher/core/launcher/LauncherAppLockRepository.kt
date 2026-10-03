package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherAppLockStore by preferencesDataStore(
    name = "launcher_app_lock",
)

enum class LauncherAppLockCredentialType {
    PIN,
    PATTERN,
}

data class LauncherAppLockState(
    val configured: Boolean = false,
    val credentialType: LauncherAppLockCredentialType? = null,
    val lockedAppKeys: Set<String> = emptySet(),
    val failedAttempts: Int = 0,
    val cooldownUntilEpochMs: Long = 0L,
) {
    fun isLocked(appKey: String): Boolean = configured && appKey in lockedAppKeys
}

sealed interface LauncherAppLockVerificationResult {
    data object Success : LauncherAppLockVerificationResult
    data object NotConfigured : LauncherAppLockVerificationResult
    data class Invalid(val attemptsRemaining: Int) : LauncherAppLockVerificationResult
    data class Cooldown(val remainingMillis: Long) : LauncherAppLockVerificationResult
}

/**
 * Local-only credential and app-lock policy store.
 *
 * Raw PINs and patterns are never persisted. The store retains only a random salt, a bounded
 * PBKDF2-HMAC-SHA256 verifier, credential type, profile-qualified Launcher workspace keys, and
 * brute-force throttle state. App Lock gates launches initiated by GoreeCloud Launcher only; it
 * does not claim Android-wide package protection.
 */
class LauncherAppLockRepository(
    private val dataStore: DataStore<Preferences>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    constructor(context: Context) : this(context.launcherAppLockStore)

    private object Keys {
        val credentialType = stringPreferencesKey("credential_type_v1")
        val credentialSalt = stringPreferencesKey("credential_salt_v1")
        val credentialVerifier = stringPreferencesKey("credential_verifier_v1")
        val lockedApps = stringPreferencesKey("locked_apps_v1")
        val failedAttempts = intPreferencesKey("failed_attempts_v1")
        val cooldownUntil = longPreferencesKey("cooldown_until_v1")
    }

    val state: Flow<LauncherAppLockState> = dataStore.data
        .map(::decodeState)
        .distinctUntilChanged()

    suspend fun configure(
        type: LauncherAppLockCredentialType,
        credential: String,
    ): Boolean {
        val normalized = normalize(type, credential) ?: return false
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val verifier = derive(normalized, salt)
        dataStore.edit { values ->
            values[Keys.credentialType] = type.name
            values[Keys.credentialSalt] = encode(salt)
            values[Keys.credentialVerifier] = encode(verifier)
            values[Keys.failedAttempts] = 0
            values[Keys.cooldownUntil] = 0L
        }
        return true
    }

    suspend fun clearCredential() {
        dataStore.edit { values ->
            values.remove(Keys.credentialType)
            values.remove(Keys.credentialSalt)
            values.remove(Keys.credentialVerifier)
            values.remove(Keys.lockedApps)
            values.remove(Keys.failedAttempts)
            values.remove(Keys.cooldownUntil)
        }
    }

    suspend fun setAppLocked(appKey: String, locked: Boolean): Boolean {
        if (appKey.isBlank()) return false
        var changed = false
        dataStore.edit { values ->
            val current = decodeState(values)
            if (!current.configured && locked) return@edit
            val updated = current.lockedAppKeys.toMutableSet()
            changed = if (locked) updated.add(appKey) else updated.remove(appKey)
            if (changed) {
                values[Keys.lockedApps] = LauncherAppLockKeyCodec.encode(
                    updated.take(MAX_LOCKED_APPS).toSet(),
                )
            }
        }
        return changed
    }

    suspend fun verify(
        type: LauncherAppLockCredentialType,
        credential: String,
    ): LauncherAppLockVerificationResult {
        val values = dataStore.data.first()
        val state = decodeState(values)
        if (!state.configured || state.credentialType == null) {
            return LauncherAppLockVerificationResult.NotConfigured
        }
        val now = nowMillis()
        if (state.cooldownUntilEpochMs > now) {
            return LauncherAppLockVerificationResult.Cooldown(
                state.cooldownUntilEpochMs - now,
            )
        }
        val normalized = normalize(type, credential)
        val salt = values[Keys.credentialSalt]?.let(::decode)
        val expected = values[Keys.credentialVerifier]?.let(::decode)
        if (
            normalized == null ||
            type != state.credentialType ||
            salt == null ||
            expected == null
        ) {
            return recordFailure(state, now)
        }

        val actual = derive(normalized, salt)
        return if (MessageDigest.isEqual(expected, actual)) {
            dataStore.edit { mutable ->
                mutable[Keys.failedAttempts] = 0
                mutable[Keys.cooldownUntil] = 0L
            }
            LauncherAppLockVerificationResult.Success
        } else {
            recordFailure(state, now)
        }
    }

    private suspend fun recordFailure(
        state: LauncherAppLockState,
        now: Long,
    ): LauncherAppLockVerificationResult {
        val baseline = if (state.cooldownUntilEpochMs <= now) 0 else state.failedAttempts
        val attempts = (baseline + 1).coerceAtMost(MAX_FAILED_ATTEMPTS)
        return if (attempts >= MAX_FAILED_ATTEMPTS) {
            val until = now + COOLDOWN_MILLIS
            dataStore.edit { values ->
                values[Keys.failedAttempts] = attempts
                values[Keys.cooldownUntil] = until
            }
            LauncherAppLockVerificationResult.Cooldown(COOLDOWN_MILLIS)
        } else {
            dataStore.edit { values ->
                values[Keys.failedAttempts] = attempts
                values[Keys.cooldownUntil] = 0L
            }
            LauncherAppLockVerificationResult.Invalid(MAX_FAILED_ATTEMPTS - attempts)
        }
    }

    private fun decodeState(values: Preferences): LauncherAppLockState {
        val type = values[Keys.credentialType]
            ?.let { raw -> LauncherAppLockCredentialType.entries.firstOrNull { it.name == raw } }
        val saltValid = values[Keys.credentialSalt]?.let(::decode)?.size == SALT_BYTES
        val verifierValid = values[Keys.credentialVerifier]?.let(::decode)?.size == KEY_BYTES
        return LauncherAppLockState(
            configured = type != null && saltValid && verifierValid,
            credentialType = type.takeIf { saltValid && verifierValid },
            lockedAppKeys = LauncherAppLockKeyCodec.decode(values[Keys.lockedApps])
                .take(MAX_LOCKED_APPS)
                .toSet(),
            failedAttempts = values[Keys.failedAttempts]
                ?.coerceIn(0, MAX_FAILED_ATTEMPTS)
                ?: 0,
            cooldownUntilEpochMs = values[Keys.cooldownUntil]?.coerceAtLeast(0L) ?: 0L,
        )
    }

    companion object {
        private const val ITERATIONS = 150_000
        private const val KEY_BITS = 256
        private const val KEY_BYTES = KEY_BITS / 8
        private const val SALT_BYTES = 16
        private const val MAX_LOCKED_APPS = 512
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val COOLDOWN_MILLIS = 30_000L

        fun normalize(
            type: LauncherAppLockCredentialType,
            credential: String,
        ): String? = when (type) {
            LauncherAppLockCredentialType.PIN ->
                credential.takeIf { it.length in 4..6 && it.all(Char::isDigit) }
            LauncherAppLockCredentialType.PATTERN ->
                credential.takeIf {
                    it.length in 4..9 &&
                        it.all { node -> node in '0'..'8' } &&
                        it.toSet().size == it.length
                }
        }

        private fun derive(normalized: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(
                normalized.toCharArray(),
                salt,
                ITERATIONS,
                KEY_BITS,
            )
            return try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .encoded
            } finally {
                spec.clearPassword()
            }
        }

        private fun encode(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        private fun decode(raw: String): ByteArray? = runCatching {
            Base64.getUrlDecoder().decode(raw)
        }.getOrNull()
    }
}

internal object LauncherAppLockKeyCodec {
    private const val SEPARATOR = "\u001E"

    fun encode(keys: Set<String>): String =
        keys.asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .joinToString(SEPARATOR) { key ->
                Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(key.toByteArray(StandardCharsets.UTF_8))
            }

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SEPARATOR)
            .mapNotNull { encoded ->
                runCatching {
                    String(
                        Base64.getUrlDecoder().decode(encoded),
                        StandardCharsets.UTF_8,
                    )
                }.getOrNull()?.takeIf { decoded ->
                    decoded.isNotBlank() &&
                        Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(decoded.toByteArray(StandardCharsets.UTF_8)) == encoded
                }
            }
            .distinct()
    }
}
