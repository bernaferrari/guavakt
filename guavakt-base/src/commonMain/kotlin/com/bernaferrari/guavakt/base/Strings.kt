package com.bernaferrari.guavakt.base

import com.bernaferrari.guavakt.annotations.GwtCompatible

@GwtCompatible
object Strings {
    /** Prefer Kotlin string templates; kept for Guava-shaped formatting. */
    fun lenientFormat(template: String, vararg args: Any?): String {
        val builder = StringBuilder(template.length + 16 * args.size)
        var templateStart = 0
        var i = 0
        while (i < args.size) {
            val placeholderStart = template.indexOf("%s", templateStart)
            if (placeholderStart == -1) break
            builder.append(template, templateStart, placeholderStart)
            builder.append(args[i++])
            templateStart = placeholderStart + 2
        }
        builder.append(template, templateStart, template.length)
        if (i < args.size) {
            builder.append(" [").append(args[i++])
            while (i < args.size) builder.append(", ").append(args[i++])
            builder.append(']')
        }
        return builder.toString()
    }

    fun nullToEmpty(string: String?): String = string ?: ""

    fun emptyToNull(string: String?): String? = if (string.isNullOrEmpty()) null else string

    fun isNullOrEmpty(string: String?): Boolean = string.isNullOrEmpty()

    fun padStart(string: String, minLength: Int, padChar: Char): String {
        Preconditions.checkNotNull(string)
        if (string.length >= minLength) return string
        val sb = StringBuilder(minLength)
        for (i in string.length until minLength) sb.append(padChar)
        sb.append(string)
        return sb.toString()
    }

    fun padEnd(string: String, minLength: Int, padChar: Char): String {
        Preconditions.checkNotNull(string)
        if (string.length >= minLength) return string
        val sb = StringBuilder(minLength)
        sb.append(string)
        for (i in string.length until minLength) sb.append(padChar)
        return sb.toString()
    }

    fun repeat(string: String, count: Int): String {
        Preconditions.checkNotNull(string)
        if (count <= 1) {
            Preconditions.checkArgument(count >= 0, "invalid count: %s", count)
            return if (count == 0) "" else string
        }
        val len = string.length.toLong()
        val longSize = len * count
        val size = longSize.toInt()
        if (size.toLong() != longSize) {
            throw IndexOutOfBoundsException("Required array size too large: $longSize")
        }
        val array = CharArray(size)
        string.toCharArray().copyInto(array, 0, 0, string.length)
        var n = string.length
        while (n < size - n) {
            array.copyInto(array, n, 0, n)
            n = n shl 1
        }
        array.copyInto(array, n, 0, size - n)
        return array.concatToString()
    }

    fun commonPrefix(a: CharSequence, b: CharSequence): String {
        Preconditions.checkNotNull(a)
        Preconditions.checkNotNull(b)
        val maxPrefixLength = minOf(a.length, b.length)
        var p = 0
        while (p < maxPrefixLength && a[p] == b[p]) p++
        if (validSurrogatePairAt(a, p - 1) || validSurrogatePairAt(b, p - 1)) p--
        return a.subSequence(0, p).toString()
    }

    fun commonSuffix(a: CharSequence, b: CharSequence): String {
        Preconditions.checkNotNull(a)
        Preconditions.checkNotNull(b)
        val maxSuffixLength = minOf(a.length, b.length)
        var s = 0
        while (s < maxSuffixLength && a[a.length - s - 1] == b[b.length - s - 1]) s++
        if (validSurrogatePairAt(a, a.length - s - 1) || validSurrogatePairAt(b, b.length - s - 1)) s--
        return a.subSequence(a.length - s, a.length).toString()
    }

    private fun validSurrogatePairAt(string: CharSequence, index: Int): Boolean =
        index >= 0 &&
            index <= string.length - 2 &&
            string[index].isHighSurrogate() &&
            string[index + 1].isLowSurrogate()
}
