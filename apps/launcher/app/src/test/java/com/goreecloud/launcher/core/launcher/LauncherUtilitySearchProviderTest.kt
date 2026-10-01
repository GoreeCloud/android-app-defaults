package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherUtilitySearchProviderTest {
    private val provider = LauncherUtilitySearchProvider()

    @Test
    fun arithmeticRespectsPrecedenceParenthesesAndUnicodeOperators() {
        assertEquals("14", provider.singleResult("2 + 3 * 4").title)
        assertEquals("20", provider.singleResult("(2 + 3) * 4").title)
        assertEquals("6", provider.singleResult("12 ÷ 2").title)
        assertEquals("15", provider.singleResult("3 × 5").title)
    }

    @Test
    fun invalidOrNonArithmeticQueriesFailClosed() {
        assertTrue(provider.search("calculator").isEmpty())
        assertTrue(provider.search("42").isEmpty())
        assertTrue(provider.search("4 / 0").isEmpty())
        assertTrue(provider.search("(2 + 3").isEmpty())
    }

    @Test
    fun convertsLengthMassVolumeTimeAndTemperatureLocally() {
        assertEquals("10 km = 6.2137119224 mi", provider.singleResult("10 km to mi").title)
        assertEquals("2 lb = 0.90718474 kg", provider.singleResult("2 lb in kg").title)
        assertEquals("1 cup = 236.5882365 mL", provider.singleResult("1 cup to ml").title)
        assertEquals("90 min = 1.5 h", provider.singleResult("90 min to h").title)
        assertEquals("32 °F = 0 °C", provider.singleResult("32 f to c").title)
        assertEquals("273.15 K = 0 °C", provider.singleResult("273.15 k to c").title)
    }

    @Test
    fun mismatchedDimensionsDoNotProduceMisleadingConversions() {
        assertTrue(provider.search("10 kg to km").isEmpty())
        assertTrue(provider.search("2 cups to hours").isEmpty())
    }

    @Test
    fun utilityResultsAreHighPriorityLocalCopyActions() {
        val result = provider.singleResult("2 + 2")

        assertEquals(LauncherSearchCategory.UTILITY, result.category)
        assertEquals(600, result.score)
        assertEquals("4", (result.action as LauncherCopyTextSearchAction).text)
    }

    @Test
    fun builtInCatalogMarksUtilityAsAutomaticLocalNoRetention() {
        val registration = LauncherBuiltInSearchProviderRegistry
            .registrations(emptyList())
            .single { it.provider.id == LauncherUtilitySearchProvider.PROVIDER_ID }
        val option = LauncherSearchProviderUserControlPolicy.optionFor(registration)

        assertEquals(LauncherSearchOfflineBehavior.LOCAL_ONLY, registration.metadata.offlineBehavior)
        assertEquals(LauncherSearchAuthorizationRequirement.NONE, registration.metadata.authorizationRequirement)
        assertEquals(LauncherSearchRemoteProcessing.NONE, registration.metadata.remoteProcessing)
        assertEquals(LauncherSearchQueryRetention.NONE, registration.metadata.queryRetention)
        assertEquals(LauncherSearchProviderInvocationMode.AUTOMATIC_LOCAL, option.invocationMode)
        assertTrue(option.defaultEnabled)
        assertEquals("Calculator & conversions", option.displayName)
    }

    private fun LauncherUtilitySearchProvider.singleResult(query: String): LauncherSearchResult =
        search(query).single()
}
