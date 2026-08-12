package com.bernaferrari.guavakt.base

internal object NullnessCasts {
    @Suppress("UNCHECKED_CAST")
    fun <T> uncheckedCastNullableTToT(t: T?): T = t as T
    fun <T> unsafeNull(): T {
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
