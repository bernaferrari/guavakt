package dev.guavakt.escape

/**
 * Guava CharEscaperBuilder — builds a char->string replacement escaper.
 */
class CharEscaperBuilder {
    private val replacements = LinkedHashMap<Char, String>()
    private var max = -1

    fun addEscape(c: Char, replacement: String): CharEscaperBuilder {
        replacements[c] = replacement
        if (c.code > max) max = c.code
        return this
    }

    fun addEscapes(chars: CharArray, replacement: String): CharEscaperBuilder {
        for (c in chars) addEscape(c, replacement)
        return this
    }

    fun toArray(): Array<String?> {
        val result = arrayOfNulls<String>(max + 1)
        for ((c, r) in replacements) result[c.code] = r
        return result
    }

    fun toEscaper(): CharEscaper {
        val array = toArray()
        return object : CharEscaper() {
            override fun escape(c: Char): CharArray? {
                if (c.code < array.size) {
                    val r = array[c.code] ?: return null
                    return r.toCharArray()
                }
                return null
            }
        }
    }
}
