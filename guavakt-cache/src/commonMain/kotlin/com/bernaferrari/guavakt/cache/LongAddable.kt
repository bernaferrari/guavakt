package com.bernaferrari.guavakt.cache

/** Guava LongAddable — long counter abstraction (striped on JVM; simple here). */
interface LongAddable {
    fun increment()
    fun add(x: Long)
    fun sum(): Long
}
