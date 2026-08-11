package dev.guavakt.util.concurrent

internal object NullnessCasts {
    @Suppress("UNCHECKED_CAST")
    fun <T> uncheckedCastNullableTToT(t: T?): T = t as T
}
