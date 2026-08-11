package dev.guavakt.base

internal object SneakyThrows {
    fun <T : Throwable> sneakyThrow(t: Throwable): Nothing {
        @Suppress("UNCHECKED_CAST")
        throw t as T
    }
}
