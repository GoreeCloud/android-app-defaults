package com.goreecloud.launcher.core.launcher

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherAiInlineSearchProviderTest {
    @Test
    fun unconfiguredProviderFailsClosedWithoutSendingQuery() = runBlocking {
        var calls = 0
        val provider = LauncherAiInlineSearchProvider(
            id = LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            displayName = "ChatGPT",
            credentialProvider = { null },
            transport = LauncherAiInlineSearchTransport { _, _ ->
                calls += 1
                "should not execute"
            },
        )

        val results = provider.searchAsync(LauncherSearchRequest("launch notes"))

        assertFalse(provider.isInlineExecutionReady)
        assertTrue(results.isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun configuredProviderMapsOneInlineAnswerToCopyAction() = runBlocking {
        var seenCredential = ""
        var seenQuery = ""
        val provider = LauncherAiInlineSearchProvider(
            id = LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID,
            displayName = "Perplexity",
            credentialProvider = { "test-key" },
            transport = LauncherAiInlineSearchTransport { credential, query ->
                seenCredential = credential
                seenQuery = query
                "  A concise\ninline answer.  "
            },
        )

        val results = provider.searchAsync(LauncherSearchRequest("latest launch status"))

        assertTrue(provider.isInlineExecutionReady)
        assertEquals("test-key", seenCredential)
        assertEquals("latest launch status", seenQuery)
        assertEquals(1, results.size)
        val result = results.single()
        assertEquals(LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID, result.providerId)
        assertEquals(LauncherSearchCategory.CONNECTED_SOURCE, result.category)
        assertEquals("A concise inline answer.", result.title)
        assertEquals("Perplexity · Inline answer · tap to copy", result.subtitle)
        assertEquals(
            "A concise inline answer.",
            (result.action as LauncherCopyTextSearchAction).text,
        )
    }

    @Test
    fun providerFailureReturnsSafeRecoveryResultWithoutErrorDetails() = runBlocking {
        val provider = LauncherAiInlineSearchProvider(
            id = LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            displayName = "ChatGPT",
            credentialProvider = { "secret-test-key" },
            transport = LauncherAiInlineSearchTransport { _, _ ->
                error("provider-specific-secret-detail")
            },
        )

        val result = provider.searchAsync(LauncherSearchRequest("status")).single()

        assertEquals("ChatGPT couldn’t load an answer", result.title)
        assertEquals(
            "Check your connection or API key · tap to review source",
            result.subtitle,
        )
        assertEquals(
            LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            (result.action as LauncherManageSearchSourceAction).providerId,
        )
        assertFalse(result.title.contains("secret"))
        assertFalse(result.subtitle.orEmpty().contains("secret"))
    }

    @Test
    fun providerCancellationStillPropagates() = runBlocking {
        val provider = LauncherAiInlineSearchProvider(
            id = LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID,
            displayName = "Google Gemini",
            credentialProvider = { "test-key" },
            transport = LauncherAiInlineSearchTransport { _, _ ->
                throw CancellationException("cancelled")
            },
        )

        var cancelled = false
        try {
            provider.searchAsync(LauncherSearchRequest("cancel me"))
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
    }

    @Test
    fun registeredAiProviderIsOptInRemoteInline() {
        val provider = LauncherAiInlineSearchProvider(
            id = LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID,
            displayName = "Claude",
            credentialProvider = { "test-key" },
            transport = LauncherAiInlineSearchTransport { _, _ -> "answer" },
        )
        val registration = LauncherSearchProviderRegistration(
            provider = provider,
            metadata = LauncherSearchProviderMetadata(
                providerId = provider.id,
                contractVersion = LauncherSearchProviderContract.currentVersion,
                provenance = LauncherSearchProviderProvenance.THIRD_PARTY,
                offlineBehavior = LauncherSearchOfflineBehavior.NETWORK_REQUIRED,
                authorizationRequirement = LauncherSearchAuthorizationRequirement.ACCOUNT,
                remoteProcessing = LauncherSearchRemoteProcessing.REQUIRED,
                queryRetention = LauncherSearchQueryRetention.UNKNOWN,
            ),
        )

        assertEquals(
            LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE,
            LauncherSearchProviderUserControlPolicy.invocationModeFor(registration),
        )
        assertEquals(
            "Claude",
            LauncherSearchProviderUserControlPolicy.displayNameFor(provider.id),
        )
    }

    @Test
    fun geminiProviderIsCredentialGatedAndNamedForSourceControls() {
        val providerId = LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID

        assertTrue(LauncherConnectedSearchProviderRegistry.isCredentialInlineProvider(providerId))
        assertEquals(
            "Google Gemini",
            LauncherSearchProviderUserControlPolicy.displayNameFor(providerId),
        )
    }
}
