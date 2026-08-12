package com.bernaferrari.guavakt.base

/** Guava JdkPattern — KMP uses [Regex] as the pattern engine. */
class JdkPattern(private val pattern: Regex) : CommonPattern() {
    override fun matcher(t: CharSequence): CommonMatcher = object : CommonMatcher() {
        private var lastMatch: MatchResult? = null
        private var searchIndex = 0
        override fun matches(): Boolean {
            lastMatch = pattern.matchEntire(t)
            return lastMatch != null
        }
        override fun find(): Boolean = find(searchIndex)
        override fun find(index: Int): Boolean {
            val m = pattern.find(t, index) ?: run {
                lastMatch = null
                return false
            }
            lastMatch = m
            searchIndex = m.range.last + 1
            return true
        }
        override fun replaceAll(replacement: String): String = pattern.replace(t, replacement)
        override fun end(): Int = (lastMatch?.range?.last ?: -2) + 1
        override fun start(): Int = lastMatch?.range?.first ?: -1
    }
    override fun flags(): Int = 0
    override fun pattern(): String = pattern.pattern
    override fun toString(): String = pattern.pattern
}
