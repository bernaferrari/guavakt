package dev.guavakt.base

abstract class CharMatcher {
    abstract fun matches(c: Char): Boolean
    open fun negate(): CharMatcher = object : CharMatcher() {
        override fun matches(c: Char): Boolean = !this@CharMatcher.matches(c)
    }
    open fun and(other: CharMatcher): CharMatcher = object : CharMatcher() {
        override fun matches(c: Char): Boolean = this@CharMatcher.matches(c) && other.matches(c)
    }
    open fun or(other: CharMatcher): CharMatcher = object : CharMatcher() {
        override fun matches(c: Char): Boolean = this@CharMatcher.matches(c) || other.matches(c)
    }
    open fun matchesAnyOf(sequence: CharSequence): Boolean {
        for (i in sequence.indices) if (matches(sequence[i])) return true
        return false
    }
    open fun matchesAllOf(sequence: CharSequence): Boolean {
        for (i in sequence.indices) if (!matches(sequence[i])) return false
        return true
    }
    open fun matchesNoneOf(sequence: CharSequence): Boolean = !matchesAnyOf(sequence)
    open fun indexIn(sequence: CharSequence): Int {
        for (i in sequence.indices) if (matches(sequence[i])) return i
        return -1
    }
    open fun indexIn(sequence: CharSequence, start: Int): Int {
        for (i in start until sequence.length) if (matches(sequence[i])) return i
        return -1
    }
    open fun lastIndexIn(sequence: CharSequence): Int {
        for (i in sequence.indices.reversed()) if (matches(sequence[i])) return i
        return -1
    }
    open fun countIn(sequence: CharSequence): Int {
        var count = 0
        for (i in sequence.indices) if (matches(sequence[i])) count++
        return count
    }
    open fun removeFrom(sequence: CharSequence): String = buildString {
        for (i in sequence.indices) if (!matches(sequence[i])) append(sequence[i])
    }
    open fun retainFrom(sequence: CharSequence): String = buildString {
        for (i in sequence.indices) if (matches(sequence[i])) append(sequence[i])
    }
    open fun replaceFrom(sequence: CharSequence, replacement: Char): String = buildString {
        for (i in sequence.indices) append(if (matches(sequence[i])) replacement else sequence[i])
    }
    open fun trimFrom(sequence: CharSequence): String {
        var first = 0
        var last = sequence.length - 1
        while (first <= last && matches(sequence[first])) first++
        while (last >= first && matches(sequence[last])) last--
        return sequence.subSequence(first, last + 1).toString()
    }
    companion object {
        fun any(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = true
        }
        fun none(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = false
        }
        fun `is`(match: Char): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c == match
        }
        fun isNot(match: Char): CharMatcher = `is`(match).negate()
        fun anyOf(sequence: CharSequence): CharMatcher {
            val set = sequence.toSet()
            return object : CharMatcher() {
                override fun matches(c: Char): Boolean = c in set
            }
        }
        fun noneOf(sequence: CharSequence): CharMatcher = anyOf(sequence).negate()
        fun inRange(startInclusive: Char, endInclusive: Char): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c in startInclusive..endInclusive
        }
        fun whitespace(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isWhitespace()
        }
        fun breakingWhitespace(): CharMatcher = whitespace()
        fun ascii(): CharMatcher = inRange('\u0000', '\u007f')
        fun digit(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isDigit()
        }
        fun javaLetter(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isLetter()
        }
        fun javaDigit(): CharMatcher = digit()
        fun javaLetterOrDigit(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isLetterOrDigit()
        }
        fun javaUpperCase(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isUpperCase()
        }
        fun javaLowerCase(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isLowerCase()
        }
        fun javaIsoControl(): CharMatcher = object : CharMatcher() {
            override fun matches(c: Char): Boolean = c.isISOControl()
        }
    }
}
