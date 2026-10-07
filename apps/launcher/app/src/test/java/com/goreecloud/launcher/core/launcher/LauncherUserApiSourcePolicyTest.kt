package com.goreecloud.launcher.core.launcher

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherUserApiSourcePolicyTest {
    @Test
    fun fourKnownApiConnectionsStartDisconnected() {
        val sources = LauncherUserApiSourcePolicy.builtInSummaries
        assertEquals(4, sources.size)
        assertEquals(4, sources.map { it.id }.distinct().size)
        assertTrue(sources.all { !it.enabled && !it.configured })
        assertTrue(LauncherUserApiSourcePolicy.isValidId(
            LauncherUserApiSourcePolicy.OPENAI_ID, LauncherUserApiKind.OPENAI,
        ))
        assertFalse(LauncherUserApiSourcePolicy.isValidId(
            LauncherUserApiSourcePolicy.OPENAI_ID, LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE,
        ))
    }

    @Test
    fun remoteCustomUrlRequiresPublicHttpsWithoutCredentialsOrQuery() {
        assertTrue(LauncherUserApiSourcePolicy.validCustomEndpoint(
            "https://user-api.example.org/v1/chat/completions",
        ))
        for (value in listOf(
            "http://api.example.org/v1/chat/completions",
            "https://user:secret@api.example.org/v1/chat/completions",
            "https://api.example.org/v1/chat/completions?key=secret",
            "https://api.example.org/v1/chat/completions#fragment",
            "https://localhost/v1/chat/completions",
            "https://workstation.local/v1/chat/completions",
            "https://api.internal/v1/chat/completions",
            "https://127.0.0.1/v1/chat/completions",
            "https://192.168.0.10/v1/chat/completions",
            "https://[::1]/v1/chat/completions",
            "https://api.example.org:8080/v1/chat/completions",
        )) {
            assertFalse(value, LauncherUserApiSourcePolicy.validCustomEndpoint(value))
        }
    }

    @Test
    fun privateDnsResolutionsAreRejectedBeforeNetworkUse() {
        fun address(value: IntArray): InetAddress = InetAddress.getByAddress(
            value.map { it.toByte() }.toByteArray(),
        )
        for (bytes in listOf(
            intArrayOf(127, 0, 0, 1),
            intArrayOf(192, 168, 1, 2),
            intArrayOf(10, 0, 0, 8),
            intArrayOf(100, 100, 2, 3),
            intArrayOf(169, 254, 169, 254),
            intArrayOf(198, 18, 0, 1),
        )) {
            assertFalse(LauncherUserApiSourcePolicy.isPublicAddress(address(bytes)))
        }
        assertTrue(
            LauncherUserApiSourcePolicy.isPublicAddress(address(intArrayOf(8, 8, 8, 8))),
        )
    }

    @Test
    fun inputValidationBoundsModelIdentityAndEndpoint() {
        val id = "api.custom.54bf95ec-f209-49da-85e0-c2ebcd276505"
        val valid = LauncherUserApiSourceInput(
            title = "My hosted assistant",
            kind = LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE,
            model = "my-chat-model-v1",
            endpoint = "https://api.example.org/v1/chat/completions",
            newApiKey = "secret",
            enabled = true,
        )
        assertEquals(null, LauncherUserApiSourcePolicy.validate(id, valid))
        assertNotNull(LauncherUserApiSourcePolicy.validate(id, valid.copy(
            endpoint = "http://router.local/admin",
        )))
        assertNotNull(LauncherUserApiSourcePolicy.validate(id, valid.copy(
            model = "../sneaky",
        )))
        assertNotNull(LauncherUserApiSourcePolicy.validate("api.custom.bad", valid))
    }

    @Test
    fun officialAdapterEndpointsAreTlsOnly() {
        for (kind in LauncherUserApiKind.entries) {
            if (kind == LauncherUserApiKind.CUSTOM_OPENAI_COMPATIBLE) continue
            val url = LauncherUserApiSourcePolicy.endpointFor(kind, "test-model", "")
            assertTrue(
                kind.name,
                LauncherUserApiSourcePolicy.validCustomEndpoint(url),
            )
        }
        assertEquals(2_000, LauncherUserApiSourcePolicy.MAX_QUERY_CHARS)
        assertEquals(6_000, LauncherUserApiSourcePolicy.MAX_ANSWER_CHARS)
    }
}
