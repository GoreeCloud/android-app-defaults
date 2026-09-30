package com.goreecloud.keyboard

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputPrivacyClassifierTest {
    @Test
    fun suppressesTextPasswordVariations() {
        assertTrue(InputPrivacyClassifier.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue(InputPrivacyClassifier.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertTrue(InputPrivacyClassifier.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
    }

    @Test
    fun suppressesNumericPasswordVariation() {
        assertTrue(InputPrivacyClassifier.isSensitive(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
    }

    @Test
    fun allowsOrdinaryText() {
        assertFalse(InputPrivacyClassifier.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL))
        assertFalse(InputPrivacyClassifier.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
    }
}
