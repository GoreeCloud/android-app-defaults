package com.goreecloud.launcher.core.launcher

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
}
