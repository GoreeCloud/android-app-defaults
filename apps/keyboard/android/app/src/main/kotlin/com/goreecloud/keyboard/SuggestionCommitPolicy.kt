package com.goreecloud.keyboard

/**
 * Fail-closed validation for replacing the local composing prefix with a selected suggestion.
 *
 * GoreeCloud Keyboard's current suggestion engine tracks only keys committed by this IME. The host
 * editor remains authoritative for the actual text/cursor state and can change it independently.
 * Before deleting a local prefix, the service must therefore confirm both that the callback refers
 * to one of the exact candidates currently presented by this editor session and that the immediately
 * preceding ordinary-field text still matches the tracked prefix. Stale or unavailable evidence is
 * never treated as permission to delete text.
 */
object SuggestionCommitPolicy {
    fun isPresentedCandidate(
        candidate: String,
        presentedCandidates: Collection<String>,
    ): Boolean = candidate.isNotEmpty() && presentedCandidates.any { it == candidate }

    fun matchesExpectedPrefix(
        expectedPrefix: String,
        actualTextBeforeCursor: CharSequence?,
    ): Boolean {
        if (expectedPrefix.isEmpty()) return true
        val actual = actualTextBeforeCursor?.toString() ?: return false
        return actual.length == expectedPrefix.length &&
            actual.equals(expectedPrefix, ignoreCase = true)
    }
}
