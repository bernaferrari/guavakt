package com.bernaferrari.guavakt.base

import com.bernaferrari.guavakt.annotations.GwtCompatible

@GwtCompatible
enum class CaseFormat {
    LOWER_HYPHEN,
    LOWER_UNDERSCORE,
    LOWER_CAMEL,
    UPPER_CAMEL,
    UPPER_UNDERSCORE;

    fun to(format: CaseFormat, str: String): String {
        Preconditions.checkNotNull(format)
        Preconditions.checkNotNull(str)
        return if (format == this) str else convert(format, str)
    }

    fun converterTo(targetFormat: CaseFormat): Converter<String, String> =
        StringConverter(this, Preconditions.checkNotNull(targetFormat))

    private fun convert(format: CaseFormat, s: String): String {
        // Fast paths for underscore/hyphen toggles
        when {
            this == LOWER_HYPHEN && format == LOWER_UNDERSCORE -> return s.replace('-', '_')
            this == LOWER_HYPHEN && format == UPPER_UNDERSCORE -> return Ascii.toUpperCase(s.replace('-', '_'))
            this == LOWER_UNDERSCORE && format == LOWER_HYPHEN -> return s.replace('_', '-')
            this == LOWER_UNDERSCORE && format == UPPER_UNDERSCORE -> return Ascii.toUpperCase(s)
            this == UPPER_UNDERSCORE && format == LOWER_HYPHEN -> return Ascii.toLowerCase(s.replace('_', '-'))
            this == UPPER_UNDERSCORE && format == LOWER_UNDERSCORE -> return Ascii.toLowerCase(s)
        }

        val sourceSeparator = wordSeparator()
        var wordStart = 0
        var boundary = -1
        var out: StringBuilder? = null
        while (true) {
            boundary = nextWordBoundary(s, boundary + 1, sourceSeparator)
            if (boundary < 0) break
            if (wordStart == 0) {
                out = StringBuilder(s.length + 4 * format.wordSeparator().length)
                out.append(format.normalizeFirstWord(s.substring(wordStart, boundary)))
            } else {
                out!!.append(format.normalizeWord(s.substring(wordStart, boundary)))
            }
            out.append(format.wordSeparator())
            wordStart = boundary + sourceSeparator.length
        }
        return if (wordStart == 0) format.normalizeFirstWord(s)
        else out!!.append(format.normalizeWord(s.substring(wordStart))).toString()
    }

    private fun nextWordBoundary(s: String, start: Int, separator: String): Int {
        if (separator.isNotEmpty()) return s.indexOf(separator, start)
        for (index in start until s.length) {
            if (Ascii.isUpperCase(s[index])) return index
        }
        return -1
    }

    private fun normalizeFirstWord(word: String): String = when (this) {
        LOWER_CAMEL -> Ascii.toLowerCase(word)
        else -> normalizeWord(word)
    }

    private fun normalizeWord(word: String): String = when (this) {
        LOWER_HYPHEN, LOWER_UNDERSCORE -> Ascii.toLowerCase(word)
        UPPER_UNDERSCORE -> Ascii.toUpperCase(word)
        LOWER_CAMEL, UPPER_CAMEL -> firstCharOnlyToUpper(word)
    }

    private fun wordSeparator(): String = when (this) {
        LOWER_HYPHEN -> "-"
        LOWER_UNDERSCORE, UPPER_UNDERSCORE -> "_"
        LOWER_CAMEL, UPPER_CAMEL -> ""
    }

    private fun firstCharOnlyToUpper(word: String): String {
        if (word.isEmpty()) return word
        return buildString {
            append(Ascii.toUpperCase(word[0]))
            if (word.length > 1) append(Ascii.toLowerCase(word.substring(1)))
        }
    }

    private class StringConverter(
        private val sourceFormat: CaseFormat,
        private val targetFormat: CaseFormat,
    ) : Converter<String, String>() {
        override fun doForward(a: String): String = sourceFormat.to(targetFormat, a)

        override fun doBackward(b: String): String = targetFormat.to(sourceFormat, b)

        override fun equals(other: Any?): Boolean =
            other is StringConverter &&
                sourceFormat == other.sourceFormat &&
                targetFormat == other.targetFormat

        override fun hashCode(): Int = sourceFormat.hashCode() xor targetFormat.hashCode()

        override fun toString(): String = "$sourceFormat.converterTo($targetFormat)"
    }
}
