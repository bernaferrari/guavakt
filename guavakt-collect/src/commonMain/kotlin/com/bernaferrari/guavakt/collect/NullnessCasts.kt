package com.bernaferrari.guavakt.collect

/** Guava NullnessCasts — unchecked nullness helpers for collections internals. */
internal object NullnessCasts {
    @Suppress("UNCHECKED_CAST")
    fun <T> uncheckedCastNullableTToT(t: T?): T = t as T

    fun <T> safeGet(map: Map<*, T>, key: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return (map as Map<Any?, T>)[key]
    }

    fun <T> safeContainsKey(map: Map<*, *>, key: Any?): Boolean = map.containsKey(key)

    fun <T> safeRemove(map: MutableMap<*, T>, key: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        return (map as MutableMap<Any?, T>).remove(key)
    }
}
