package com.goreecloud.gallery

import com.goreecloud.gallery.android.AndroidMediaMutationMode
import com.goreecloud.gallery.android.AndroidMediaMutationPendingState
import com.goreecloud.gallery.android.AndroidMediaMutationPendingStates
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Versioned Activity-state envelope for an already-authorized Android MediaStore mutation.
 *
 * The fingerprint is an integrity check for ordinary lifecycle persistence only; it is not an
 * authentication mechanism or a substitute for Android MediaStore confirmation authority.
 * Restoration still routes through [GalleryMediaMutationPendingPolicy]. Activity integration is a
 * separate lifecycle step so this contract can be validated independently before persisted UI
 * state depends on it.
 */
internal class GalleryMediaMutationSavedState internal constructor(
    val schemaVersion: Int,
    val modeName: String,
    contentUris: Array<String>,
    val fingerprint: String,
) {
    private val frozenContentUris = contentUris.copyOf()

    fun contentUriValues(): Array<String> = frozenContentUris.copyOf()
}

internal object GalleryMediaMutationSavedStates {
    const val SCHEMA_VERSION = 1
    const val FINGERPRINT_HEX_LENGTH = 64

    fun capture(state: AndroidMediaMutationPendingState): GalleryMediaMutationSavedState {
        require(state.mode == AndroidMediaMutationMode.TRASH || state.mode == AndroidMediaMutationMode.DELETE) {
            "main Gallery saved state may retain only trash or permanent delete authority"
        }
        val modeName = AndroidMediaMutationPendingStates.modeName(state)
        val contentUris = AndroidMediaMutationPendingStates.contentUriValues(state)
        return GalleryMediaMutationSavedState(
            schemaVersion = SCHEMA_VERSION,
            modeName = modeName,
            contentUris = contentUris,
            fingerprint = fingerprint(SCHEMA_VERSION, modeName, contentUris.asList()),
        )
    }

    fun restore(
        schemaVersion: Int?,
        modeName: String?,
        contentUris: Collection<String>?,
        fingerprint: String?,
    ): AndroidMediaMutationPendingState? {
        if (
            schemaVersion != SCHEMA_VERSION ||
            modeName == null ||
            contentUris == null ||
            fingerprint == null
        ) return null

        val suppliedUris = contentUris.toList()
        if (
            fingerprint.length != FINGERPRINT_HEX_LENGTH ||
            fingerprint != fingerprint(schemaVersion, modeName, suppliedUris)
        ) return null

        return GalleryMediaMutationPendingPolicy.restore(modeName, suppliedUris)
    }

    private fun fingerprint(
        schemaVersion: Int,
        modeName: String,
        contentUris: Collection<String>,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")

        fun add(value: String) {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
            digest.update(bytes)
        }

        add(schemaVersion.toString())
        add(modeName)
        contentUris.forEach(::add)
        return digest.digest().toLowerHex()
    }

    private fun ByteArray.toLowerHex(): String {
        val alphabet = "0123456789abcdef"
        val result = CharArray(size * 2)
        forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            result[index * 2] = alphabet[value ushr 4]
            result[index * 2 + 1] = alphabet[value and 0x0f]
        }
        return String(result)
    }
}
