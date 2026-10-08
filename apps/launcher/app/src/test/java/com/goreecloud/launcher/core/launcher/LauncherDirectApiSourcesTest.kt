package com.goreecloud.launcher.core.launcher

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDirectApiSourcesTest {
    @Test
    fun officialApiConnectionsRemainOffUntilExplicitConfiguration() {
        val sources = LauncherDirectApiCatalog.templates()
        assertEquals(
            setOf(
                LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
                LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID,
                LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID,
                LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID,
            ),
            sources.map { it.id }.toSet(),
        )
        assertTrue(sources.all { !it.enabled && it.secret.isEmpty() })
    }

    @Test
    fun officialDestinationCannotBeReplacedBySavedPhishingEndpoint() {
        val official = LauncherDirectApiCatalog.templates().first()
        val altered = official.copy(
            endpoint = "https://unrelated.example/v1/chat/completions",
            title = "Misleading title",
        )
        val resolved = LauncherDirectApiCatalog.resolveSaved(altered)
        assertEquals(official.endpoint, resolved.endpoint)
        assertEquals(official.title, resolved.title)
        assertEquals(official.kind, resolved.kind)
    }

    @Test
    fun customApisRequireHttpsAndRejectUrlCredentialsAndFragments() {
        assertTrue(launcherDirectApiValidHttpsEndpoint("https://api.example.com/v1/chat/completions"))
        assertTrue(launcherDirectApiValidHttpsEndpoint("https://search.example.com/query"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("http://api.example.com/chat"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://user:pass@api.example.com/chat"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://api.example.com/chat#fragment"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://api.example.com/chat?q=secret"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("file:///etc/passwd"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://"))
    }

    @Test
    fun customEndpointAndResolvedAddressRejectLocalTargets() {
        for (endpoint in listOf(
            "https://localhost/api",
            "https://device.local/search",
            "https://metadata.google.internal/latest",
            "https://127.0.0.1/api",
            "https://192.168.1.40/api",
            "https://[::1]/api",
            "https://public.example.org:8080/api",
        )) {
            assertFalse(endpoint, launcherDirectApiValidHttpsEndpoint(endpoint))
        }

        fun inet(vararg octets: Int): InetAddress =
            InetAddress.getByAddress(octets.map { it.toByte() }.toByteArray())

        for (address in listOf(
            inet(0, 0, 0, 0),
            inet(10, 1, 2, 3),
            inet(127, 0, 0, 1),
            inet(169, 254, 169, 254),
            inet(172, 16, 0, 4),
            inet(192, 168, 1, 1),
            inet(100, 100, 1, 2),
            inet(198, 18, 0, 1),
            inet(224, 0, 0, 1),
        )) {
            assertFalse(launcherDirectApiIsPublicAddress(address))
        }
        assertTrue(launcherDirectApiIsPublicAddress(inet(8, 8, 8, 8)))
    }

    @Test
    fun customHeadersAndJsonPathsRejectControlAndSeparatorCharacters() {
        assertTrue(launcherDirectApiValidFieldName("X-API-Key"))
        assertTrue(launcherDirectApiValidFieldName("Authorization"))
        assertFalse(launcherDirectApiValidFieldName("X-API-Key\r\nHost"))
        assertFalse(launcherDirectApiValidFieldName(""))
        assertTrue(launcherDirectApiValidResponsePath("results.0.snippet"))
        assertTrue(launcherDirectApiValidResponsePath("answer"))
        assertFalse(launcherDirectApiValidResponsePath("results..snippet"))
        assertFalse(launcherDirectApiValidResponsePath("answers[0].text"))
    }

    @Test
    fun providerHttpFailuresExposeStatusOnlyAndNeverReadErrorBodies() = runBlocking {
        for (status in listOf(302, 401, 429, 503)) {
            val connection = FakeHttpURLConnection(
                url = URL("https://api.example.com/search"),
                status = status,
                responseBytes = "provider-body-must-stay-private".toByteArray(),
            )
            val error = runCatching {
                testClient(connection).answer(testSearchSource(), "hello")
            }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Provider returned HTTP $status", error?.message)
            assertFalse(error?.message.orEmpty().contains("provider-body-must-stay-private"))
            assertFalse(connection.inputStreamOpened)
            assertFalse(connection.instanceFollowRedirects)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun oversizedProviderResponseFailsClosedAndDisconnects() = runBlocking {
        val connection = FakeHttpURLConnection(
            url = URL("https://api.example.com/search"),
            status = 200,
            responseBytes = ByteArray(96_001) { 'x'.code.toByte() },
        )
        val error = runCatching {
            testClient(connection).answer(testSearchSource(), "hello")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Provider response exceeds the size limit", error?.message)
        assertTrue(connection.disconnected)
    }

    @Test
    fun dnsLookupTimeoutFailsBeforeAnyConnectionOpens() = runBlocking {
        var connectionOpened = false
        val client = LauncherDirectApiAnswerClient(
            addressResolver = {
                Thread.sleep(5_000)
                arrayOf(publicAddress())
            },
            connectionFactory = {
                connectionOpened = true
                FakeHttpURLConnection(it, 200, ByteArray(0))
            },
            dnsTimeoutMillis = 50,
        )

        val error = runCatching {
            client.answer(testSearchSource(), "hello")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Provider DNS lookup timed out", error?.message)
        assertFalse(connectionOpened)
    }

    @Test
    fun cancellationInterruptsBlockedResponseReadAndDisconnects() = runBlocking {
        val readStarted = CountDownLatch(1)
        val connection = FakeHttpURLConnection(
            url = URL("https://api.example.com/search"),
            status = 200,
            responseStreamFactory = {
                object : InputStream() {
                    override fun read(): Int {
                        val one = ByteArray(1)
                        return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
                    }

                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        readStarted.countDown()
                        try {
                            Thread.sleep(60_000)
                        } catch (_: InterruptedException) {
                            throw InterruptedIOException("interrupted")
                        }
                        return -1
                    }
                }
            },
        )
        val job = launch {
            testClient(connection).answer(testSearchSource(), "hello")
        }

        assertTrue(readStarted.await(2, TimeUnit.SECONDS))
        withTimeout(2_000) {
            job.cancelAndJoin()
        }
        assertTrue(connection.disconnected)
    }

    private fun testSearchSource(): LauncherDirectApiSource = LauncherDirectApiSource(
        id = "custom.fixture",
        kind = LauncherDirectApiKind.CUSTOM_SEARCH,
        title = "Fixture Search API",
        endpoint = "https://api.example.com/search",
        model = "",
        secret = "fixture-secret",
        enabled = true,
        searchParameter = "q",
        responsePath = "answer",
    )

    private fun testClient(connection: FakeHttpURLConnection): LauncherDirectApiAnswerClient =
        LauncherDirectApiAnswerClient(
            addressResolver = { arrayOf(publicAddress()) },
            connectionFactory = { connection },
        )

    private fun publicAddress(): InetAddress =
        InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))

    private class FakeHttpURLConnection(
        url: URL,
        private val status: Int,
        private val responseStreamFactory: () -> InputStream,
    ) : HttpURLConnection(url) {
        constructor(
            url: URL,
            status: Int,
            responseBytes: ByteArray,
        ) : this(url, status, { ByteArrayInputStream(responseBytes) })

        var disconnected = false
        var inputStreamOpened = false

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy(): Boolean = false

        override fun connect() {
            connected = true
        }

        override fun getResponseCode(): Int = status

        override fun getInputStream(): InputStream {
            inputStreamOpened = true
            return responseStreamFactory()
        }
    }
}
