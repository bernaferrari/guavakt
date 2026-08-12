package com.bernaferrari.guavakt.io

/** Guava PatternFilenameFilter — accepts filenames matching a regex. */
class PatternFilenameFilter(pattern: String) {
    private val regex = Regex(pattern)
    fun accept(dir: String?, name: String): Boolean = regex.containsMatchIn(name)
    fun accept(name: String): Boolean = accept(null, name)
}
