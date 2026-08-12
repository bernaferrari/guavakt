package com.bernaferrari.guavakt.cache

/**
 * Guava ReferenceEntry — hash table entry for LocalCache (strong refs on KMP).
 */
internal interface ReferenceEntry<K, V> {
    fun getKey(): K?
    fun getHash(): Int
    fun getNext(): ReferenceEntry<K, V>?
    fun getValueReference(): ValueReference<K, V>?
    fun setValueReference(valueReference: ValueReference<K, V>)
    fun getAccessTime(): Long
    fun setAccessTime(time: Long)
    fun getWriteTime(): Long
    fun setWriteTime(time: Long)
}

internal interface ValueReference<K, V> {
    fun get(): V?
    fun getWeight(): Int
    fun getEntry(): ReferenceEntry<K, V>?
    fun isLoading(): Boolean
    fun isActive(): Boolean
}

internal class StrongEntry<K, V>(
    private val key: K,
    private val hash: Int,
    private val next: ReferenceEntry<K, V>?,
) : ReferenceEntry<K, V> {
    private var valueRef: ValueReference<K, V>? = null
    private var accessTime = Long.MAX_VALUE
    private var writeTime = Long.MAX_VALUE
    override fun getKey(): K = key
    override fun getHash(): Int = hash
    override fun getNext(): ReferenceEntry<K, V>? = next
    override fun getValueReference(): ValueReference<K, V>? = valueRef
    override fun setValueReference(valueReference: ValueReference<K, V>) { valueRef = valueReference }
    override fun getAccessTime(): Long = accessTime
    override fun setAccessTime(time: Long) { accessTime = time }
    override fun getWriteTime(): Long = writeTime
    override fun setWriteTime(time: Long) { writeTime = time }
}

internal class StrongValueReference<K, V>(private val referent: V) : ValueReference<K, V> {
    override fun get(): V = referent
    override fun getWeight(): Int = 1
    override fun getEntry(): ReferenceEntry<K, V>? = null
    override fun isLoading(): Boolean = false
    override fun isActive(): Boolean = true
}
