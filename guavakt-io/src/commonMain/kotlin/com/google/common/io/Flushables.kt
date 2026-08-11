package dev.guavakt.io

/** Guava Flushables — flush helpers that optionally swallow IO errors. */
object Flushables {
    interface Flushable { fun flush() }

    fun flush(flushable: Flushable, swallowIOException: Boolean) {
        try {
            flushable.flush()
        } catch (e: Exception) {
            if (!swallowIOException) throw e
        }
    }

    fun flushQuietly(flushable: Flushable) = flush(flushable, true)
}
