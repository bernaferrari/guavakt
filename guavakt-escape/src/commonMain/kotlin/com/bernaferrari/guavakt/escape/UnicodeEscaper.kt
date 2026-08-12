package com.bernaferrari.guavakt.escape

abstract class UnicodeEscaper : Escaper() {
    override fun escape(string: String): String {
        val end = string.length
        val index = nextEscapeIndex(string, 0, end)
        return if (index == end) string else escapeSlow(string, index)
    }

    /** Returns a replacement for one valid Unicode code point, or null when it is safe. */
    protected abstract fun escape(cp: Int): CharArray?
    internal fun escapeInternal(cp: Int): CharArray? = escape(cp)

    /**
     * Finds the next UTF-16 index whose complete Unicode code point may need escaping.
     * Malformed surrogate sequences are rejected instead of being silently treated as characters.
     */
    protected open fun nextEscapeIndex(csq: CharSequence, start: Int, end: Int): Int {
        var index = start
        while (index < end) {
            val codePoint = codePointAt(csq, index, end)
            if (codePoint < 0 || escape(codePoint) != null) break
            index += charCount(codePoint)
        }
        return index
    }

    /** Escapes [s] from the first index that may require a replacement. */
    protected fun escapeSlow(s: String, index: Int): String {
        val end = s.length
        return buildString {
            var current = index
            var unescapedStart = 0
            while (current < end) {
                val codePoint = codePointAt(s, current, end)
                require(codePoint >= 0) { "Trailing high surrogate at end of input" }
                val replacement = escape(codePoint)
                val next = current + charCount(codePoint)
                if (replacement != null) {
                    append(s, unescapedStart, current)
                    append(replacement)
                    unescapedStart = next
                }
                current = nextEscapeIndex(s, next, end)
            }
            append(s, unescapedStart, end)
        }
    }

    private fun charCount(codePoint: Int): Int = if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

    private fun codePointAt(sequence: CharSequence, index: Int, end: Int): Int {
        if (index >= end) throw IndexOutOfBoundsException("Index exceeds specified range")
        val first = sequence[index]
        if (first !in HIGH_SURROGATE_RANGE && first !in LOW_SURROGATE_RANGE) return first.code
        if (first in HIGH_SURROGATE_RANGE) {
            if (index + 1 == end) return -first.code
            val second = sequence[index + 1]
            require(second in LOW_SURROGATE_RANGE) {
                "Expected low surrogate but got char '$second' with value ${second.code} at index ${index + 1} in '$sequence'"
            }
            return MIN_SUPPLEMENTARY_CODE_POINT +
                ((first.code - HIGH_SURROGATE_RANGE.first.code) shl 10) +
                (second.code - LOW_SURROGATE_RANGE.first.code)
        }
        throw IllegalArgumentException(
            "Unexpected low surrogate character '$first' with value ${first.code} at index $index in '$sequence'",
        )
    }

    private companion object {
        const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
        val HIGH_SURROGATE_RANGE = '\uD800'..'\uDBFF'
        val LOW_SURROGATE_RANGE = '\uDC00'..'\uDFFF'
    }
}
