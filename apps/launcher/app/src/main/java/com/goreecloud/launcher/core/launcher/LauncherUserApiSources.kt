package com.goreecloud.launcher.core.launcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.net.HttpURLConnection
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-owned API credentials are entirely separate from ordinary provider preferences, portable
 * Launcher backup, analytics, logs, Search history, and Android profile/workspace state.
 *
 * Only a deliberate "Ask" tap invokes these providers. In particular, this path NEVER joins
 * the live per-keystroke Universal Search aggregation or the app-handoff registry.
 */
enum class LauncherUserApiKind(val title: String) {
    OPENAI("ChatGPT API"),
    ANTHROPIC("Claude API"),
    GEMINI("Gemini API"),
    PERPLEXITY("Perplexity API"),
    CUSTOM_OPENAI_COMPATIBLE("Custom chat/search API"),
}

data class LauncherUserApiSourceSummary(
    val id: String,
    val title: String,
    val kind: LauncherUserApiKind,
    val model: String,
    val endpoint: String,
    val enabled: Boolean,
    val configured: Boolean,
)

data class LauncherUserApiSourceInput(
    val title: String,
    val kind: LauncherUserApiKind,
    val model: String,
    val endpoint: String,
    /** Empty on edit means preserve the existing credential; never echo an existing secret to UI. */
    val newApiKey: String,
    val enabled: Boolean,
)

object LauncherUserApiSourcePolicy {
    const val OPENAI_ID = "api.chatgpt"
    const val ANTHROPIC_ID = "api.claude"
    const val GEMINI_ID = "api.gemini"
    const val PERPLEXITY_ID = "api.perplexity"
    const val MAX_CUSTOM_SOURCES = 8
    const val MAX_QUERY_CHARS = 2_000
    const val MAX_ANSWER_CHARS = 6_000

    private val modelPattern = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,119}")
    private val customIdPattern = Regex("api\\.custom\\.[a-f0-9-]{36}")
    private val ipV4Pattern = Regex("[0-9]+(\\.[0-9]+){3}")
    private val hostLabel = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")
    private val builtIns = listOf(
        OPENAI_ID to LauncherUserApiKind.OPENAI,
        ANTHROPIC_ID to LauncherUserApiKind.ANTHROPIC,
        GEMINI_ID to LauncherUserApiKind.GEMINI,
        PERPLEXITY_ID to LauncherUserApiKind.PERPLEXITY,
    )

    val builtInSummaries: List<LauncherUserApiSourceSummary> = builtIns.map { (id, kind) ->
        LauncherUserApiSourceSummary(id, kind.title, kind, "", "", false, false)
    }

    fun isValidId(id: String, kind: LauncherUserApiKind): Boolean = if (
        kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE
    ) {
        customIdPattern.matches(id)
    } else {
        builtIns.any { (knownId, knownKind) -> id == knownId && kind == knownKind }
    }

    fun validModel(model: String): Boolean = modelPattern.matches(model.trim())

    /** Custom endpoints must be publicly addressed HTTPS domains, not device/LAN URLs. */
    fun validCustomEndpoint(raw: String): Boolean {
        if (raw.length !in 12..300) return false
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase()?.removeSuffix(".") ?: return false
        if (host.length > 253 || host == "localhost" || host == "metadata.google.internal") {
            return false
        }
        if (host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".internal") || host.endsWith(".test") ||
            host.endsWith(".invalid") || host.endsWith(".example") ||
            host.endsWith(".onion")
        ) return false
        if (host.contains(':') || ipV4Pattern.matches(host)) return false
        val labels = host.split('.')
        if (labels.size < 2 || labels.any { !hostLabel.matches(it) }) return false
        if (uri.rawPath.orEmpty().length > 200 || uri.rawPath.orEmpty().contains('\\')) return false
        return true
    }

    fun validate(id: String, input: LauncherUserApiSourceInput): String? {
        if (!isValidId(id, input.kind)) return "Unsupported API source identity"
        if (input.title.trim().length !in 1..50) return "Name must contain 1–50 characters"
        if (!validModel(input.model)) return "Enter a valid API model ID"
        if (input.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE &&
            !validCustomEndpoint(input.endpoint.trim())
        ) return "Enter a public HTTPS endpoint without URL credentials or query parameters"
        if (input.newApiKey.length > 4_096) return "API key is too long"
        return null
    }

    fun endpointFor(kind: LauncherUserApiKind, model: String, custom: String): String = when (kind) {
        LauncherUserApiKind.OPENAI -> "https://api.openai.com/v1/chat/completions"
        LauncherUserApiKind.ANTHROPIC -> "https://api.anthropic.com/v1/messages"
        LauncherUserApiKind.GEMINI ->
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        LauncherUserApiKind.PERPLEXITY -> "https://api.perplexity.ai/chat/completions"
        LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE -> custom
    }

    /** DNS preflight is a second boundary; redirects remain disabled to avoid host switching. */
    internal fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) return false
        val octets = address.address
        if (address is Inet6Address) {
            return octets.size == 16 && (octets[0].toInt() and 0xfe) != 0xfc
        }
        if (octets.size != 4) return false
        val a = octets[0].toInt() and 0xff
        val b = octets[1].toInt() and 0xff
        if (a == 0 || a >= 224) return false
        if (a == 100 && b in 64..127) return false // shared/private address space
        if (a == 192 && b == 0) return false
        if (a == 198 && b in 18..19) return false // benchmarking/private networks
        return true
    }
}

private data class LauncherUserApiStoredSource(
    val id: String,
    val title: String,
    val kind: LauncherUserApiKind,
    val model: String,
    val endpoint: String,
    val apiKey: String,
    val enabled: Boolean,
) {
    fun summary(): LauncherUserApiSourceSummary = LauncherUserApiSourceSummary(
        id = id, title = title, kind = kind, model = model,
        endpoint = endpoint, enabled = enabled, configured = apiKey.isNotBlank(),
    )
}

/**
 * Entire payload (including configuration and credentials) is AES-256-GCM encrypted using an
 * Android Keystore key. The AtomicFile lives under noBackupFilesDir, not SharedPreferences,
 * DataStore, Room, portable restore, or Android cloud/device-transfer backups.
 */
class LauncherUserApiSourceRepository(context: Context) {
    private val app = context.applicationContext
    private val file = AtomicFile(File(app.noBackupFilesDir, "launcher_search_user_apis.v1"))
    private val mutex = Mutex()
    private val mutableSources = MutableStateFlow(LauncherUserApiSourcePolicy.builtInSummaries)
    val sources = mutableSources.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { publish(readStored()) }
    }

    suspend fun save(id: String, input: LauncherUserApiSourceInput) = withContext(Dispatchers.IO) {
        LauncherUserApiSourcePolicy.validate(id, input)?.let { throw IllegalArgumentException(it) }
        mutex.withLock {
            val stored = readStored().toMutableList()
            val previous = stored.firstOrNull { it.id == id }
            val key = input.newApiKey.trim().ifBlank { previous?.apiKey.orEmpty() }
            require(key.isNotBlank()) { "Enter an API key to connect this provider" }
            if (input.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE &&
                previous == null && stored.count {
                    it.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE
                } >= LauncherUserApiSourcePolicy.MAX_CUSTOM_SOURCES
            ) throw IllegalArgumentException("Maximum custom API sources reached")
            val next = LauncherUserApiStoredSource(
                id, input.title.trim(), input.kind, input.model.trim(),
                if (input.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE) {
                    input.endpoint.trim()
                } else {
                    ""
                },
                key, input.enabled,
            )
            stored.removeAll { it.id == id }
            stored.add(next)
            writeStored(stored)
            publish(stored)
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val previous = readStored()
            if (previous.none { it.id == id }) return@withLock
            val next = previous.map { if (it.id == id) it.copy(enabled = enabled) else it }
            writeStored(next)
            publish(next)
        }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = readStored().filterNot { it.id == id }
            writeStored(next)
            publish(next)
        }
    }

    /**
     * Execute only from an explicit user gesture. Neither the source list nor the live Search
     * provider catalog can invoke this method by typing a query or enabling a switch.
     */
    suspend fun ask(id: String, rawQuery: String): String = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        require(query.length in 1..LauncherUserApiSourcePolicy.MAX_QUERY_CHARS) {
            "Question is empty or too long"
        }
        val source = mutex.withLock {
            readStored().firstOrNull { it.id == id && it.enabled && it.apiKey.isNotBlank() }
        } ?: throw IllegalStateException("This API source is not connected and enabled")
        val endpoint = LauncherUserApiSourcePolicy.endpointFor(
            source.kind, source.model, source.endpoint,
        )
        require(LauncherUserApiSourcePolicy.validCustomEndpoint(endpoint)) {
            "API endpoint is not an approved public HTTPS address"
        }
        val hostname = checkNotNull(URI(endpoint).host)
        val addresses = InetAddress.getAllByName(hostname)
        require(addresses.isNotEmpty() && addresses.all(LauncherUserApiSourcePolicy::isPublicAddress)) {
            "The API endpoint does not resolve to a public address"
        }
        postQuestion(source, endpoint, query)
    }

    private fun publish(stored: List<LauncherUserApiStoredSource>) {
        val configured = stored.map { it.summary() }
        val byId = configured.associateBy { it.id }
        mutableSources.value = LauncherUserApiSourcePolicy.builtInSummaries.map {
            byId[it.id] ?: it
        } + configured.filter { it.kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun readStored(): List<LauncherUserApiStoredSource> {
        if (!file.baseFile.exists()) return emptyList()
        val data = file.openRead().use { it.readBytes() }
        require(data.size > 1 + IV_LENGTH + 16 && data[0] == 1.toByte()) {
            "API source store has an unsupported format"
        }
        val iv = data.copyOfRange(1, 1 + IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD)
        val plaintext = cipher.doFinal(data.copyOfRange(1 + IV_LENGTH, data.size))
        val json = JSONObject(String(plaintext, Charsets.UTF_8))
        require(json.optInt("version") == 1) { "Unsupported API source storage revision" }
        val array = json.getJSONArray("sources")
        require(array.length() <= 12) { "Too many stored API sources" }
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val kind = LauncherUserApiKind.valueOf(obj.getString("kind"))
                val id = obj.getString("id")
                val entry = LauncherUserApiStoredSource(
                    id = id,
                    title = obj.getString("title"),
                    kind = kind,
                    model = obj.getString("model"),
                    endpoint = obj.optString("endpoint"),
                    apiKey = obj.getString("apiKey"),
                    enabled = obj.optBoolean("enabled"),
                )
                require(LauncherUserApiSourcePolicy.validate(
                    id,
                    LauncherUserApiSourceInput(
                        entry.title, entry.kind, entry.model, entry.endpoint, "", entry.enabled,
                    ),
                ) == null) { "Stored API source is invalid" }
                require(none { it.id == id }) { "Duplicate stored API source" }
                add(entry)
            }
        }
    }

    private fun writeStored(stored: List<LauncherUserApiStoredSource>) {
        val list = JSONArray()
        stored.forEach { source ->
            list.put(
                JSONObject()
                    .put("id", source.id)
                    .put("title", source.title)
                    .put("kind", source.kind.name)
                    .put("model", source.model)
                    .put("endpoint", source.endpoint)
                    .put("apiKey", source.apiKey)
                    .put("enabled", source.enabled),
            )
        }
        val plaintext = JSONObject().put("version", 1).put("sources", list)
            .toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(AAD)
        val output = ByteBuffer.allocate(1 + IV_LENGTH + cipher.getOutputSize(plaintext.size))
            .put(1.toByte())
            .put(cipher.iv)
            .put(cipher.doFinal(plaintext))
            .array()
        val stream = file.startWrite()
        try {
            stream.write(output)
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun postQuestion(source: LauncherUserApiStoredSource, endpoint: String, query: String): String {
        val message = JSONObject().put("role", "user").put("content", query)
        val request = when (source.kind) {
            LauncherUserApiKind.ANTHROPIC -> JSONObject()
                .put("model", source.model)
                .put("max_tokens", 512)
                .put("messages", JSONArray().put(message))
            LauncherUserApiKind.GEMINI -> JSONObject()
                .put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", query))),
                ))
            else -> JSONObject()
                .put("model", source.model)
                .put("messages", JSONArray().put(message))
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 9_000
            readTimeout = 12_000
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            when (source.kind) {
                LauncherUserApiKind.ANTHROPIC -> {
                    setRequestProperty("x-api-key", source.apiKey)
                    setRequestProperty("anthropic-version", "2023-06-01")
                }
                LauncherUserApiKind.GEMINI -> setRequestProperty("x-goog-api-key", source.apiKey)
                else -> setRequestProperty("Authorization", "Bearer " + source.apiKey)
            }
        }
        try {
            connection.outputStream.use { output ->
                output.write(request.toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                // Do not expose a provider error body: it may echo prompts or credentials.
                throw IllegalStateException("Provider returned HTTP $status")
            }
            val bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4_096)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 131_072) {
                        "API reply exceeds the allowed size"
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val answer = extractAnswer(
                source.kind,
                JSONObject(String(bytes, Charsets.UTF_8)),
            ).trim()
            require(answer.isNotBlank()) { "Provider returned no answer text" }
            return answer.take(LauncherUserApiSourcePolicy.MAX_ANSWER_CHARS)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractAnswer(kind: LauncherUserApiKind, json: JSONObject): String = when (kind) {
        LauncherUserApiKind.ANTHROPIC -> {
            val blocks = json.optJSONArray("content") ?: JSONArray()
            (0 until blocks.length())
                .mapNotNull { blocks.optJSONObject(it)?.optString("text") }
                .filter { it.isNotBlank() }.joinToString("\n")
        }
        LauncherUserApiKind.GEMINI -> {
            val parts = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
            (0 until parts.length())
                .mapNotNull { parts.optJSONObject(it)?.optString("text") }
                .filter { it.isNotBlank() }.joinToString("\n")
        }
        else -> json.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content").orEmpty()
    }

    private companion object {
        const val KEY_ALIAS = "goreecloud_launcher_user_api_sources_v1"
        const val IV_LENGTH = 12
        val AAD = "GoreeCloudLauncherUserApiSourcesV1".toByteArray(Charsets.UTF_8)
    }
}
