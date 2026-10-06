package com.goreecloud.launcher.core.launcher

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Connected Search remains fail closed. Handoff providers never receive live typed input, and an
 * opt-in remote-inline provider receives it only after both explicit source enablement and its own
 * credential/authorization readiness gate are satisfied.
 */
@RunWith(AndroidJUnit4::class)
class LauncherConnectedSearchPrivacyRuntimeTest {
    @Test
    fun driveIsDiscoverableYetDisabledByDefaultAndCannotAutoSearch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val registrations = LauncherConnectedSearchProviderRegistry.registrations(context)
        val driveId = LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID
        assertTrue(registrations.any { it.metadata.providerId == driveId })

        val catalog = LauncherSearchProviderContract.evaluate(registrations)
        val defaults = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )
        assertTrue(defaults.orderedOptions.any { it.providerId == driveId })
        assertFalse(defaults.isEnabled(driveId))
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, defaults).isEmpty())

        val optedIn = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf(driveId),
            requestedProviderOrder = listOf(driveId),
        )
        assertTrue(optedIn.isEnabled(driveId))
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, optedIn).isEmpty())
    }

    @Test
    fun connectedSourceCatalogKeepsKnownSourcesVisibleWhenOptionalAppsAreUnavailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val registrations = LauncherConnectedSearchProviderRegistry.registrations(context)
        val providerIds = registrations.map { it.metadata.providerId }.toSet()

        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID,
            ),
        )
        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID,
            ),
        )
        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID,
            ),
        )
        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            ),
        )
        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID,
            ),
        )
        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID,
            ),
        )
        assertTrue(
            providerIds.contains(
                LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID,
            ),
        )

        val dropboxId = LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID
        if (!LauncherConnectedSearchProviderRegistry.isExplicitHandoffAvailable(context, dropboxId)) {
            assertNull(
                LauncherConnectedSearchProviderRegistry.buildExplicitHandoffIntent(
                    context,
                    dropboxId,
                    "project notes",
                ),
            )
        }
    }

    @Test
    fun remoteInlineAiRequiresBothOptInAndKeystoreCredential() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providerId = LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID
        LauncherConnectedSearchCredentialStore.clear(context, providerId)

        try {
            val catalog = LauncherSearchProviderContract.evaluate(
                LauncherConnectedSearchProviderRegistry.registrations(context),
            )
            val optedIn = LauncherSearchProviderUserControlPolicy.normalize(
                catalog = catalog,
                requestedEnabledProviderIds = setOf(providerId),
                requestedProviderOrder = listOf(providerId),
            )

            assertTrue(optedIn.isEnabled(providerId))
            assertFalse(
                LauncherSearchProviderUserControlPolicy
                    .automaticProviders(catalog, optedIn)
                    .any { provider -> provider.id == providerId },
            )

            LauncherConnectedSearchCredentialStore.save(
                context,
                providerId,
                "runtime-test-key",
            )

            assertTrue(LauncherConnectedSearchCredentialStore.isConfigured(context, providerId))
            assertEquals(
                "runtime-test-key",
                LauncherConnectedSearchCredentialStore.read(context, providerId),
            )
            assertTrue(
                LauncherSearchProviderUserControlPolicy
                    .automaticProviders(catalog, optedIn)
                    .any { provider -> provider.id == providerId },
            )
        } finally {
            LauncherConnectedSearchCredentialStore.clear(context, providerId)
        }

        assertFalse(LauncherConnectedSearchCredentialStore.isConfigured(context, providerId))
    }

    @Test
    fun explicitDriveHandoffEncodesQueryButRejectsBlankInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val driveId = LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID
        assertNull(
            LauncherConnectedSearchProviderRegistry.buildExplicitHandoffIntent(
                context, driveId, "   ",
            ),
        )

        val intent = LauncherConnectedSearchProviderRegistry.buildExplicitHandoffIntent(
            context, driveId, "camera receipts",
        )
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent!!.action)
        assertEquals("https", intent.data!!.scheme)
        assertEquals("drive.google.com", intent.data!!.host)
        assertEquals("camera receipts", intent.data!!.getQueryParameter("q"))
    }
}
