package com.goreecloud.keyboard

/**
 * Keeps the transient local suggestion prefix within a small, explicit observation boundary.
 *
 * The keyboard still commits every key to the host editor normally. This policy only decides
 * whether the IME may continue retaining a local prefix for candidate generation. Once the bound
 * would be exceeded, the service drops that prefix and keeps suggestions disabled until a word or
 * editor boundary resets capture; it never starts a new mid-word prefix from incomplete context.
 */
object SuggestionCapturePolicy {
    const val MAX_PREFIX_CODE_POINTS = 64

    fun canAppend(
        currentPrefix: String,
        addition: String,
        maxCodePoints: Int = MAX_PREFIX_CODE_POINTS,
    ): Boolean {
        require(maxCodePoints > 0) { "maxCodePoints must be positive" }
        val currentCount = currentPrefix.codePointCount(0, currentPrefix.length)
        val additionCount = addition.codePointCount(0, addition.length)
        return currentCount <= maxCodePoints - additionCount
    }
}
