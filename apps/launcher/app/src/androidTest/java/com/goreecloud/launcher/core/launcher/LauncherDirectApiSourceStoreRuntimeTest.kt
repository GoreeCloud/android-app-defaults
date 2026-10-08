package com.goreecloud.launcher.core.launcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** On-device proof that user-owned API credentials remain encrypted and fail closed on key loss. */
@RunWith(AndroidJUnit4::class)
class LauncherDirectApiSourceStoreRuntimeTest {
    @Test
    fun credentialDoesNotAppearInStoredBytesAndDisconnectDeletesIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val fileName = "direct_api_sources_test_$suffix.enc"
        val keyAlias = "goreecloud_launcher_search_direct_api_test_$suffix"
        val store = LauncherDirectApiSourceStore(
            context = context,
            fileName = fileName,
            keyAlias = keyAlias,
        )
        val id = LauncherDirectApiCatalog.nextCustomId()
        val fakeKey = "fixture-secret-" + UUID.randomUUID().toString()
        val file = File(context.noBackupFilesDir, fileName)
        val source = LauncherDirectApiSource(
            id = id,
            kind = LauncherDirectApiKind.CUSTOM_CHAT,
            title = "Test custom chat source",
            endpoint = "https://api.example.org/v1/chat/completions",
            model = "test-model",
            secret = fakeKey,
            enabled = false,
        )
        try {
            store.upsert(source)
            assertTrue(store.list().any {
                it.id == id && it.secret == fakeKey && !it.enabled
            })
            assertTrue(file.exists())
            assertTrue(file.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath))
            assertFalse(String(file.readBytes(), Charsets.ISO_8859_1).contains(fakeKey))
            assertFalse(source.toString().contains(fakeKey))

            store.remove(id)
            assertFalse(store.list().any { it.id == id })
        } finally {
            store.resetAll()
        }
        assertFalse(file.exists())
    }

    @Test
    fun staleCiphertextAfterKeystoreLossFailsClosedAndExplicitResetRecovers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val fileName = "direct_api_sources_test_$suffix.enc"
        val keyAlias = "goreecloud_launcher_search_direct_api_test_$suffix"
        val store = LauncherDirectApiSourceStore(
            context = context,
            fileName = fileName,
            keyAlias = keyAlias,
        )
        val file = File(context.noBackupFilesDir, fileName)
        val fakeKey = "fixture-secret-" + UUID.randomUUID().toString()
        val source = LauncherDirectApiSource(
            id = LauncherDirectApiCatalog.nextCustomId(),
            kind = LauncherDirectApiKind.CUSTOM_CHAT,
            title = "Keystore loss fixture",
            endpoint = "https://api.example.org/v1/chat/completions",
            model = "test-model",
            secret = fakeKey,
            enabled = false,
        )

        try {
            store.upsert(source)
            val staleCiphertext = file.readBytes()
            assertFalse(String(staleCiphertext, Charsets.ISO_8859_1).contains(fakeKey))

            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            assertTrue(keyStore.containsAlias(keyAlias))
            keyStore.deleteEntry(keyAlias)

            val failure = runCatching { store.list() }.exceptionOrNull()
            assertTrue("Stale ciphertext must not decrypt after Keystore loss", failure != null)
            assertFalse(failure.toString().contains(fakeKey))
            assertTrue(file.exists())

            store.resetAll()
            assertFalse(file.exists())

            val replacementSecret = "replacement-secret-" + UUID.randomUUID().toString()
            store.upsert(source.copy(secret = replacementSecret, enabled = true))
            assertTrue(store.list().any {
                it.id == source.id && it.secret == replacementSecret && it.enabled
            })
            assertFalse(
                String(file.readBytes(), Charsets.ISO_8859_1).contains(replacementSecret),
            )
        } finally {
            store.resetAll()
        }
        assertFalse(file.exists())
    }
}


/** Android-runtime coverage for response parsing and request construction unavailable to JVM stubs. */
@RunWith(AndroidJUnit4::class)
class LauncherDirectApiAnswerClientRuntimeTest {
    @Test
    fun malformedSuccessJsonFailsWithGenericMessageAndDisconnects() = runBlocking {
        val connection = DirectApiFakeHttpURLConnection(
            url = URL("https://api.example.com/chat"),
            status = 200,
            responseBytes = "not-json provider-private-text".toByteArray(),
        )
        val error = runCatching {
            runtimeClient(connection).answer(runtimeChatSource(), "hello")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Provider returned invalid JSON", error?.message)
        assertFalse(error?.message.orEmpty().contains("provider-private-text"))
        assertTrue(connection.disconnected)
    }

    @Test
    fun successfulFixtureUsesExpectedPostAndParsesAnswer() = runBlocking {
        val connection = DirectApiFakeHttpURLConnection(
            url = URL("https://api.example.com/chat"),
            status = 200,
            responseBytes = runtimeValidAnswerBytes(),
        )

        val answer = runtimeClient(connection).answer(runtimeChatSource(), "hello")

        assertEquals("fixture answer", answer)
        assertEquals("POST", connection.requestMethod)
        assertEquals("Bearer fixture-secret", connection.getRequestProperty("Authorization"))
        assertTrue(
            connection.requestBody.toString(StandardCharsets.UTF_8.name())
                .contains("\"content\":\"hello\""),
        )
        assertTrue(connection.disconnected)
    }

    private fun runtimeChatSource(): LauncherDirectApiSource = LauncherDirectApiSource(
        id = "custom.runtime.fixture",
        kind = LauncherDirectApiKind.CUSTOM_CHAT,
        title = "Runtime fixture API",
        endpoint = "https://api.example.com/chat",
        model = "fixture-model",
        secret = "fixture-secret",
        enabled = true,
    )

    private fun runtimeClient(
        connection: DirectApiFakeHttpURLConnection,
    ): LauncherDirectApiAnswerClient = LauncherDirectApiAnswerClient(
        addressResolver = { arrayOf(runtimePublicAddress()) },
        connectionFactory = { connection },
    )

    private fun runtimePublicAddress(): InetAddress =
        InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))

    private fun runtimeValidAnswerBytes(): ByteArray =
        """{"choices":[{"message":{"content":"fixture answer"}}]}"""
            .toByteArray(StandardCharsets.UTF_8)
}

private class DirectApiFakeHttpURLConnection(
    url: URL,
    private val status: Int,
    private val responseStreamFactory: () -> InputStream,
) : HttpURLConnection(url) {
    constructor(
        url: URL,
        status: Int,
        responseBytes: ByteArray,
    ) : this(url, status, { ByteArrayInputStream(responseBytes) })

    val requestBody = ByteArrayOutputStream()
    var disconnected = false

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun connect() {
        connected = true
    }

    override fun getResponseCode(): Int = status

    override fun getInputStream(): InputStream = responseStreamFactory()

    override fun getOutputStream(): ByteArrayOutputStream = requestBody
}
