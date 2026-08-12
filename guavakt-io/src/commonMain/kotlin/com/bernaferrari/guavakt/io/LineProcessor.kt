package com.bernaferrari.guavakt.io

/** Incrementally consumes logical lines; returning `false` stops the enclosing read. */
interface LineProcessor<T> {
    fun processLine(line: String): Boolean
    fun getResult(): T
}
