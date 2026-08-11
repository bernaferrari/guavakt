package dev.guavakt.escape

abstract class CharEscaper : Escaper() {
    override fun escape(string: String): String {
        val length = string.length
        for (index in 0 until length) {
            if (escape(string[index]) != null) return escapeSlow(string, index)
        }
        return string
    }
    protected abstract fun escape(c: Char): CharArray?
    internal fun escapeInternal(c: Char): CharArray? = escape(c)

    /** Escapes [s] from the first index that may require a replacement. */
    protected fun escapeSlow(s: String, index: Int): String {
        val length = s.length
        var dest = CharArray(length * 2)
        var destIndex = 0
        var lastEscape = 0
        var i = index
        while (i < length) {
            val r = escape(s[i])
            if (r != null) {
                val charsSkipped = i - lastEscape
                val sizeNeeded = destIndex + charsSkipped + r.size
                if (dest.size < sizeNeeded) {
                    val newDest = CharArray(maxOf(dest.size * 2 + 2, sizeNeeded))
                    dest.copyInto(newDest, 0, 0, destIndex)
                    dest = newDest
                }
                for (j in 0 until charsSkipped) dest[destIndex++] = s[lastEscape + j]
                for (ch in r) dest[destIndex++] = ch
                lastEscape = i + 1
            }
            i++
        }
        val charsLeft = length - lastEscape
        if (charsLeft > 0) {
            val sizeNeeded = destIndex + charsLeft
            if (dest.size < sizeNeeded) {
                val newDest = CharArray(sizeNeeded)
                dest.copyInto(newDest, 0, 0, destIndex)
                dest = newDest
            }
            for (j in 0 until charsLeft) dest[destIndex++] = s[lastEscape + j]
        }
        return dest.concatToString(0, destIndex)
    }
}
