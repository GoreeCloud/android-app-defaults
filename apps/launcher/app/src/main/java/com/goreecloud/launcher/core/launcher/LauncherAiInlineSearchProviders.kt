package com.goreecloud.launcher.core.launcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * App-private, non-backup credential storage for optional remote-inline Search providers.
 *
 * Provider keys are encrypted with a non-exportable Android Keystore AES key and ciphertext is
 * stored under noBackupFilesDir so Android backup/device transfer cannot export it.
 */
object LauncherConnectedSearchCredentialStore {
    private const val KEY_ALIAS = "goreecloud.launcher.connected-search.credentials.v1"
    private const val FILE_NAME = "connected-search-credentials-v1.json"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val lock = Any()
    private val revisionState = MutableStateFlow(0L)

    val revision = revisionState.asStateFlow()

    fun isConfigured(context: Context, providerId: String): Boolean =
        read(context, providerId) != null

    internal fun read(context: Context, providerId: String): String? = synchronized(lock) {
        if (!LauncherConnectedSearchProviderRegistry.isCredentialInlineProvider(providerId)) return null
        val file = credentialFile(context)
        if (!file.isFile) return null
        val decrypted = runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val entry = root.optJSONObject(providerId) ?: return@runCatching null
            val iv = Base64.decode(entry.getString("iv"), Base64.NO_WRAP)
            val encrypted = Base64.decode(entry.getString("ciphertext"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8).takeIf { it.isNotBlank() }
        }.getOrNull()

        if (decrypted == null) {
            val root = readCiphertextRoot(context)
            if (root.has(providerId)) {
                root.remove(providerId)
                writeCiphertextRoot(context, root)
                revisionState.value += 1L
            }
        }
        decrypted
    }

    fun save(context: Context, providerId: String, apiKey: String) {
        require(LauncherConnectedSearchProviderRegistry.isCredentialInlineProvider(providerId)) {
            "Unsupported connected Search credential provider"
        }
        val normalized = apiKey.trim()
        require(normalized.isNotEmpty()) { "API key must not be blank" }
        require(normalized.length <= 4096) { "API key is unexpectedly long" }

        synchronized(lock) {
            val root = readCiphertextRoot(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
            root.put(
                providerId,
                JSONObject()
                    .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                    .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)),
            )
            writeCiphertextRoot(context, root)
            revisionState.value += 1L
        }
    }

    fun clear(context: Context, providerId: String) {
        if (!LauncherConnectedSearchProviderRegistry.isCredentialInlineProvider(providerId)) return
        synchronized(lock) {
            val root = readCiphertextRoot(context)
            if (!root.has(providerId)) return
            root.remove(providerId)
            writeCiphertextRoot(context, root)
            revisionState.value += 1L
        }
    }
    private fun credentialFile(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, FILE_NAME)

    private fun readCiphertextRoot(context: Context): JSONObject {
        val file = credentialFile(context)
        if (!file.isFile) return JSONObject()
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrElse { JSONObject() }
    }

    private fun writeCiphertextRoot(context: Context, root: JSONObject) {
        val atomicFile = AtomicFile(credentialFile(context))
        val stream = atomicFile.startWrite()
        try {
            stream.write(root.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
            atomicFile.finishWrite(stream)
        } catch (failure: Throwable) {
            atomicFile.failWrite(stream)
            throw failure
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build(),
                )
            }
            .generateKey()
    }
}

internal fun interface LauncherAiInlineSearchTransport {
    suspend fun answer(apiKey: String, rawQuery: String): String
}

internal class LauncherAiInlineSearchProvider(
    override val id: String,
    private val displayName: String,
    private val credentialProvider: () -> String?,
    private val transport: LauncherAiInlineSearchTransport,
) : LauncherOptInRemoteInlineSearchProvider {
    override val isInlineExecutionReady: Boolean
        get() = !credentialProvider().isNullOrBlank()

    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(request: LauncherSearchRequest): List<LauncherSearchResult> {
        val key = withContext(Dispatchers.IO) {
            credentialProvider()?.takeIf { it.isNotBlank() }
        } ?: return emptyList()
        val query = request.rawQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.isBlank()) return emptyList()
        val answer = try {
            transport.answer(key, query)
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(MAX_ANSWER_LENGTH)
                .takeIf(String::isNotBlank)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return listOf(
                LauncherSearchResult(
                    providerId = id,
                    resultId = "$id:error:" + query.hashCode().toUInt().toString(16),
                    title = "$displayName couldn’t load an answer",
                    subtitle = "Check your connection or API key · tap to review source",
                    category = LauncherSearchCategory.CONNECTED_SOURCE,
                    score = 55,
                    action = LauncherManageSearchSourceAction(id),
                ),
            )
        } ?: return emptyList()

        return listOf(
            LauncherSearchResult(
                providerId = id,
                resultId = "$id:answer:" + query.hashCode().toUInt().toString(16),
                title = answer,
                subtitle = "$displayName · Inline answer · tap to copy",
                category = LauncherSearchCategory.CONNECTED_SOURCE,
                score = 260,
                action = LauncherCopyTextSearchAction(answer),
            ),
        )
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 400
        const val MAX_ANSWER_LENGTH = 1200
    }
}

internal abstract class LauncherBoundedJsonHttpTransport(
    private val endpoint: String,
    private val connectTimeoutMillis: Int = 6_000,
    private val readTimeoutMillis: Int = 8_000,
) : LauncherAiInlineSearchTransport {
    final override suspend fun answer(
        apiKey: String,
        rawQuery: String,
    ): String = withContext(Dispatchers.IO) {
        val requestBody = buildRequest(rawQuery).toString().toByteArray(Charsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = false
            doOutput = true
            setFixedLengthStreamingMode(requestBody.size)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            applyAuthentication(this, apiKey)
        }
        val cancellationHandle = coroutineContext.job.invokeOnCompletion {
            connection.disconnect()
        }

        try {
            coroutineContext.ensureActive()
            connection.outputStream.use { it.write(requestBody) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Connected AI Search provider request failed")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(MAX_RESPONSE_CHARS + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = reader.read(buffer, total, buffer.size - total)
                    if (read < 0) break
                    total += read
                }
                if (total > MAX_RESPONSE_CHARS) {
                    throw IllegalStateException("Connected AI Search provider response exceeded limit")
                }
                String(buffer, 0, total)
            }
            coroutineContext.ensureActive()
            parseAnswer(JSONObject(body))
        } finally {
            cancellationHandle.dispose()
            connection.disconnect()
        }
    }

    protected abstract fun buildRequest(rawQuery: String): JSONObject

    protected abstract fun applyAuthentication(
        connection: HttpURLConnection,
        credential: String,
    )

    protected abstract fun parseAnswer(body: JSONObject): String

    private companion object {
        const val MAX_RESPONSE_CHARS = 65_536
    }
}

internal class LauncherOpenAiResponsesTransport :
    LauncherBoundedJsonHttpTransport("https://api.openai.com/v1/responses") {
    override fun buildRequest(rawQuery: String): JSONObject =
        JSONObject()
            .put("model", "chat-latest")
            .put("instructions", "Answer the Launcher Universal Search query directly and concisely. Return plain text only.")
            .put("input", rawQuery)
            .put("max_output_tokens", 160)
            .put("store", false)

    override fun applyAuthentication(
        connection: HttpURLConnection,
        credential: String,
    ) {
        connection.setRequestProperty("Authorization", "Bearer $credential")
    }

    override fun parseAnswer(body: JSONObject): String =
        parseResponseStyleOutputText(body)
}

internal class LauncherAnthropicMessagesTransport :
    LauncherBoundedJsonHttpTransport("https://api.anthropic.com/v1/messages") {
    override fun buildRequest(rawQuery: String): JSONObject =
        JSONObject()
            .put("model", "claude-sonnet-5")
            .put("max_tokens", 160)
            .put(
                "system",
                "Answer the Launcher Universal Search query directly and concisely. Return plain text only.",
            )
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", rawQuery),
                ),
            )

    override fun applyAuthentication(
        connection: HttpURLConnection,
        credential: String,
    ) {
        connection.setRequestProperty("x-api-key", credential)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
    }

    override fun parseAnswer(body: JSONObject): String {
        val content = body.optJSONArray("content") ?: return ""
        return buildString {
            for (index in 0 until content.length()) {
                val item = content.optJSONObject(index) ?: continue
                if (item.optString("type") != "text") continue
                val text = item.optString("text").trim()
                if (text.isBlank()) continue
                if (isNotEmpty()) append(' ')
                append(text)
            }
        }
    }
}

internal class LauncherPerplexityAgentTransport :
    LauncherBoundedJsonHttpTransport("https://api.perplexity.ai/v1/agent") {
    override fun buildRequest(rawQuery: String): JSONObject =
        JSONObject()
            .put("preset", "fast")
            .put("instructions", "Answer the Launcher Universal Search query directly and concisely. Return plain text only.")
            .put("input", rawQuery)

    override fun applyAuthentication(
        connection: HttpURLConnection,
        credential: String,
    ) {
        connection.setRequestProperty("Authorization", "Bearer $credential")
    }

    override fun parseAnswer(body: JSONObject): String {
        return when (body.optString("status")) {
            "failed", "cancelled", "incomplete" -> ""
            else -> parseResponseStyleOutputText(body)
        }
    }
}

internal class LauncherGeminiGenerateContentTransport :
    LauncherBoundedJsonHttpTransport(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent",
    ) {
    override fun buildRequest(rawQuery: String): JSONObject =
        JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", rawQuery)),
                        ),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("maxOutputTokens", 256)
                    .put(
                        "thinkingConfig",
                        JSONObject().put("thinkingLevel", "low"),
                    ),
            )

    override fun applyAuthentication(
        connection: HttpURLConnection,
        credential: String,
    ) {
        connection.setRequestProperty("x-goog-api-key", credential)
    }

    override fun parseAnswer(body: JSONObject): String {
        val candidate = body.optJSONArray("candidates")?.optJSONObject(0) ?: return ""
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        return buildString {
            for (index in 0 until parts.length()) {
                val text = parts.optJSONObject(index)?.optString("text").orEmpty().trim()
                if (text.isBlank()) continue
                if (isNotEmpty()) append(' ')
                append(text)
            }
        }
    }
}

internal fun parseResponseStyleOutputText(body: JSONObject): String {
    val output = body.optJSONArray("output") ?: return ""
    return buildString {
        for (outputIndex in 0 until output.length()) {
            val outputItem = output.optJSONObject(outputIndex) ?: continue
            val content = outputItem.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val item = content.optJSONObject(contentIndex) ?: continue
                if (item.optString("type") != "output_text") continue
                val text = item.optString("text").trim()
                if (text.isBlank()) continue
                if (isNotEmpty()) append(' ')
                append(text)
            }
        }
    }
}
