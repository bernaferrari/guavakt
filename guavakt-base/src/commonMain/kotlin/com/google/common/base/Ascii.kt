package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible

@GwtCompatible
object Ascii {
    const val MIN = 0.toChar()
    const val MAX = 127.toChar()

    fun isLowerCase(c: Char): Boolean = c in 'a'..'z'
    fun isUpperCase(c: Char): Boolean = c in 'A'..'Z'

    fun toLowerCase(c: Char): Char = if (isUpperCase(c)) (c.code xor 0x20).toChar() else c
    fun toUpperCase(c: Char): Char = if (isLowerCase(c)) (c.code xor 0x20).toChar() else c

    fun toLowerCase(string: CharSequence): String {
        val length = string.length
        for (i in 0 until length) {
            if (isUpperCase(string[i])) {
                val chars = CharArray(length) { idx ->
                    val c = string[idx]
                    if (isUpperCase(c)) (c.code xor 0x20).toChar() else c
                }
                return chars.concatToString()
            }
        }
        return string.toString()
    }

    fun toUpperCase(string: CharSequence): String {
        val length = string.length
        for (i in 0 until length) {
            if (isLowerCase(string[i])) {
                val chars = CharArray(length) { idx ->
                    val c = string[idx]
                    if (isLowerCase(c)) (c.code xor 0x20).toChar() else c
                }
                return chars.concatToString()
            }
        }
        return string.toString()
    }

    fun equalsIgnoreCase(s1: CharSequence, s2: CharSequence): Boolean {
        val length = s1.length
        if (s1 === s2) return true
        if (length != s2.length) return false
        for (i in 0 until length) {
            val c1 = s1[i]
            val c2 = s2[i]
            if (c1 == c2) continue
            val alphaIndex1 = getAlphaIndex(c1)
            if (alphaIndex1 < 26 && alphaIndex1 == getAlphaIndex(c2)) continue
            return false
        }
        return true
    }

    private fun getAlphaIndex(c: Char): Int {
        val upper = (c.code or 0x20) - 'a'.code
        return if (upper in 0..25) upper else 26
    }

    fun truncate(seq: CharSequence, maxLength: Int, truncationIndicator: String): String {
        Preconditions.checkNotNull(seq)
        val truncationLength = maxLength - truncationIndicator.length
        Preconditions.checkArgument(
            truncationLength >= 0,
            "maxLength (%s) must be >= length of the truncation indicator (%s)",
            maxLength,
            truncationIndicator.length,
        )
        if (seq.length <= maxLength) {
            val string = seq.toString()
            if (string.length <= maxLength) return string
        }
        return buildString {
            append(seq, 0, truncationLength)
            append(truncationIndicator)
        }
    }
}
