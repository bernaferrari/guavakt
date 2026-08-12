package com.bernaferrari.guavakt.base

/** Guava CommonPattern — abstract pattern (KMP). */
abstract class CommonPattern {
    abstract fun matcher(t: CharSequence): CommonMatcher
    abstract fun flags(): Int
    abstract fun pattern(): String
    companion object {
        fun compile(pattern: String): CommonPattern = JdkPattern(Regex(pattern))
        fun isPcreLike(): Boolean = true
    }
}
