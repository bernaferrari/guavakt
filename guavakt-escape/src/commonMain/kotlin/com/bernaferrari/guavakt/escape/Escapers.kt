package com.bernaferrari.guavakt.escape

object Escapers {
    private val nullEscaper = object : CharEscaper() {
        override fun escape(string: String): String = string
        override fun escape(c: Char): CharArray? = null
    }

    fun nullEscaper(): Escaper = nullEscaper

    fun builder(): Builder = Builder()

    class Builder {
        private val replacements = LinkedHashMap<Char, String>()
        private var safeMin: Char = Char.MIN_VALUE
        private var safeMax: Char = Char.MAX_VALUE
        private var unsafeReplacement: String? = null

        fun addEscape(c: Char, replacement: String): Builder = apply { replacements[c] = replacement }
        fun setSafeRange(min: Char, max: Char): Builder = apply {
            safeMin = min
            safeMax = max
        }
        fun setUnsafeReplacement(replacement: String?): Builder = apply {
            unsafeReplacement = replacement
        }
        fun build(): Escaper {
            val replacement = unsafeReplacement?.toCharArray()
            return object : ArrayBasedCharEscaper(replacements, safeMin, safeMax) {
                override fun escapeUnsafe(c: Char): CharArray? = replacement
            }
        }
    }

    fun computeReplacement(escaper: CharEscaper, c: Char): String? =
        escaper.escapeInternal(c)?.concatToString()

    fun computeReplacement(escaper: UnicodeEscaper, codePoint: Int): String? =
        escaper.escapeInternal(codePoint)?.concatToString()
}
