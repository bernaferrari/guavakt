package com.bernaferrari.guavakt.collect

/** Guava SneakyThrows — rethrow checked exceptions without declaring them. */
internal object SneakyThrows {
    fun <T> sneakyThrow(t: Throwable): T {
        throw sneakyThrow0(t)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Throwable> sneakyThrow0(t: Throwable): T {
        throw t as T
    }
}
