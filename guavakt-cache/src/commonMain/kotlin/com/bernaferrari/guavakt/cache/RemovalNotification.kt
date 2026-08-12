package com.bernaferrari.guavakt.cache

/** Guava RemovalNotification — key/value + removal cause. */
class RemovalNotification<K, V> private constructor(
    private val key: K?,
    private val value: V?,
    val cause: RemovalCause,
) {
    fun getKey(): K? = key
    fun getValue(): V? = value
    fun wasEvicted(): Boolean = cause.wasEvicted()

    companion object {
        fun <K, V> create(key: K?, value: V?, cause: RemovalCause): RemovalNotification<K, V> =
            RemovalNotification(key, value, cause)
    }
}
