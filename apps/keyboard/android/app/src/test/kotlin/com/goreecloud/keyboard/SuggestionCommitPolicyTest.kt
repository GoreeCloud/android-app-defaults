package com.goreecloud.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionCommitPolicyTest {
    @Test
    fun exactPresentedCandidateMayReachHostPrefixValidation() {
        assertTrue(
            SuggestionCommitPolicy.isPresentedCandidate(
                candidate = "hello",
                presentedCandidates = listOf("hello", "help", "held"),
            ),
        )
    }

    @Test
    fun staleEmptyOrCaseAlteredCandidateIsNotPresentationAuthority() {
        val presented = listOf("hello", "help")
        assertFalse(SuggestionCommitPolicy.isPresentedCandidate("world", presented))
        assertFalse(SuggestionCommitPolicy.isPresentedCandidate("", presented))
        assertFalse(SuggestionCommitPolicy.isPresentedCandidate("Hello", presented))
        assertFalse(SuggestionCommitPolicy.isPresentedCandidate("hello", emptyList()))
    }

    @Test
    fun exactCurrentPrefixMayBeReplaced() {
        assertTrue(SuggestionCommitPolicy.matchesExpectedPrefix("hello", "hello"))
    }

    @Test
    fun shiftedCommittedPrefixStillMatchesLocalLowercaseTracking() {
        assertTrue(SuggestionCommitPolicy.matchesExpectedPrefix("hello", "Hello"))
    }

    @Test
    fun changedHostTextFailsClosed() {
        assertFalse(SuggestionCommitPolicy.matchesExpectedPrefix("hello", "jello"))
    }

    @Test
    fun shorterOrUnavailableHostTextFailsClosed() {
        assertFalse(SuggestionCommitPolicy.matchesExpectedPrefix("hello", "ello"))
        assertFalse(SuggestionCommitPolicy.matchesExpectedPrefix("hello", null))
    }

    @Test
    fun emptyLocalPrefixRequiresNoHostReplacementEvidence() {
        assertTrue(SuggestionCommitPolicy.matchesExpectedPrefix("", null))
    }
}
