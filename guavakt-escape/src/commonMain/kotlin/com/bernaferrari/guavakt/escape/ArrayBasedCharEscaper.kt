package com.bernaferrari.guavakt.escape

/**
 * A fast UTF-16 [CharEscaper] with explicit replacements and an inclusive safe range.
 *
 * Explicit replacements take priority over the safe range. If [safeMax] is less than [safeMin],
 * the range is empty and every character without a replacement is passed to [escapeUnsafe].
 */
abstract class ArrayBasedCharEscaper : CharEscaper {
    private val replacements: Array<CharArray?>
    private val safeMin: Char
    private val safeMax: Char

    protected constructor(
        replacementMap: Map<Char, String>,
        safeMin: Char,
        safeMax: Char,
    ) : this(ArrayBasedEscaperMap.create(replacementMap), safeMin, safeMax)

    protected constructor(
        escaperMap: ArrayBasedEscaperMap,
        safeMin: Char,
        safeMax: Char,
    ) {
        replacements = escaperMap.replacementArray()
        if (safeMax < safeMin) {
            this.safeMin = Char.MAX_VALUE
            this.safeMax = Char.MIN_VALUE
        } else {
            this.safeMin = safeMin
            this.safeMax = safeMax
        }
    }

    final override fun escape(string: String): String {
        for (index in string.indices) {
            val character = string[index]
            if (
                (character.code < replacements.size && replacements[character.code] != null) ||
                character < safeMin || character > safeMax
            ) {
                return escapeSlow(string, index)
            }
        }
        return string
    }

    final override fun escape(c: Char): CharArray? {
        if (c.code < replacements.size) {
            replacements[c.code]?.let { return it }
        }
        return if (c in safeMin..safeMax) null else escapeUnsafe(c)
    }

    protected abstract fun escapeUnsafe(c: Char): CharArray?
}
