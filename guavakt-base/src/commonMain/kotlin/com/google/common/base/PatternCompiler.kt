package dev.guavakt.base

/** Guava PatternCompiler SPI. */
interface PatternCompiler {
    fun compile(pattern: String): CommonPattern
    fun isPcreLike(): Boolean
}
