package dev.guavakt.escape

import kotlin.math.min

/**
 * A Unicode-aware escaper with fast BMP replacements and an inclusive safe code-point range.
 *
 * Explicit UTF-16 character replacements take priority over the safe range. Supplementary
 * characters are decoded as complete Unicode code points before [escapeUnsafe] is consulted.
 * Malformed surrogate input throws [IllegalArgumentException]. The [unsafeReplacement] constructor
 * parameter is retained for Guava-shaped source migration; as in Guava, subclasses define the
 * actual fallback in [escapeUnsafe].
 */
@Suppress("UNUSED_PARAMETER")
abstract class ArrayBasedUnicodeEscaper : UnicodeEscaper {
    private val replacements: Array<CharArray?>
    private val safeMin: Int
    private val safeMax: Int
    private val safeMinChar: Char
    private val safeMaxChar: Char

    protected constructor(
        replacementMap: Map<Char, String>,
        safeMin: Int,
        safeMax: Int,
        unsafeReplacement: String?,
    ) : this(ArrayBasedEscaperMap.create(replacementMap), safeMin, safeMax, unsafeReplacement)

    protected constructor(
        escaperMap: ArrayBasedEscaperMap,
        safeMin: Int,
        safeMax: Int,
        unsafeReplacement: String?,
    ) {
        replacements = escaperMap.replacementArray()
        if (safeMax < safeMin) {
            this.safeMin = Int.MAX_VALUE
            this.safeMax = -1
        } else {
            this.safeMin = safeMin
            this.safeMax = safeMax
        }
        if (this.safeMin >= MIN_HIGH_SURROGATE) {
            safeMinChar = Char.MAX_VALUE
            safeMaxChar = Char.MIN_VALUE
        } else {
            safeMinChar = this.safeMin.toChar()
            safeMaxChar = min(this.safeMax, MIN_HIGH_SURROGATE - 1).toChar()
        }
    }

    final override fun escape(string: String): String {
        for (index in string.indices) {
            val character = string[index]
            if (
                (character.code < replacements.size && replacements[character.code] != null) ||
                character < safeMinChar || character > safeMaxChar
            ) {
                return escapeSlow(string, index)
            }
        }
        return string
    }

    final override fun escape(cp: Int): CharArray? {
        if (cp in replacements.indices) {
            replacements[cp]?.let { return it }
        }
        return if (cp in safeMin..safeMax) null else escapeUnsafe(cp)
    }

    final override fun nextEscapeIndex(csq: CharSequence, start: Int, end: Int): Int {
        var index = start
        while (index < end) {
            val character = csq[index]
            if (
                (character.code < replacements.size && replacements[character.code] != null) ||
                character < safeMinChar || character > safeMaxChar
            ) {
                break
            }
            index++
        }
        return index
    }

    protected abstract fun escapeUnsafe(cp: Int): CharArray?

    private companion object {
        const val MIN_HIGH_SURROGATE = 0xD800
    }
}
