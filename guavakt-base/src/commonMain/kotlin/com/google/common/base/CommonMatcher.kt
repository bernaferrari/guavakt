package dev.guavakt.base

/** Guava CommonMatcher — matcher facade over a pattern match result. */
abstract class CommonMatcher {
    abstract fun matches(): Boolean
    abstract fun find(): Boolean
    abstract fun find(index: Int): Boolean
    abstract fun replaceAll(replacement: String): String
    abstract fun end(): Int
    abstract fun start(): Int
}
