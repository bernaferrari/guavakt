package com.bernaferrari.guavakt.cache

/** Guava LongAddables — factory for LongAddable (pure synchronized counter on KMP). */
object LongAddables {
    fun create(): LongAddable = object : LongAddable {
        private var value = 0L
        private val lock = Any()
        override fun increment() { monitorSync(lock) { value++ } }
        override fun add(x: Long) { monitorSync(lock) { value += x } }
        override fun sum(): Long = monitorSync(lock) { value }
    }
}
