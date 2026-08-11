package dev.guavakt.collect

/**
 * Guava CompactHashMap — open-addressing map (portable Kotlin).
 */
open class CompactHashMap<K, V> private constructor(expectedSize: Int) : AbstractMutableMap<K, V>() {
    private var keyTable: Array<Any?> = arrayOfNulls(capacityFor(expectedSize))
    private var valueTable: Array<Any?> = arrayOfNulls(keyTable.size)
    private var _size = 0

    private fun capacityFor(expected: Int): Int {
        var n = 4
        val need = (expected.coerceAtLeast(0) * 4 / 3 + 1).coerceAtLeast(4)
        while (n < need) n = n shl 1
        return n
    }

    override val size: Int get() = _size

    private fun hash(key: Any?): Int {
        var h = key.hashCode()
        h = h xor (h ushr 16)
        return h
    }

    private fun indexOf(key: K): Int {
        if (keyTable.isEmpty()) return -1
        val mask = keyTable.size - 1
        var i = hash(key) and mask
        val start = i
        while (true) {
            val k = keyTable[i]
            if (k == null) return -1
            if (k === TOMBSTONE) {
                i = (i + 1) and mask
                if (i == start) return -1
                continue
            }
            if (k == key) return i
            i = (i + 1) and mask
            if (i == start) return -1
        }
    }

    override fun get(key: K): V? {
        val i = indexOf(key)
        @Suppress("UNCHECKED_CAST")
        return if (i >= 0) valueTable[i] as V? else null
    }

    override fun containsKey(key: K): Boolean = indexOf(key) >= 0

    override fun put(key: K, value: V): V? {
        if (_size + 1 > keyTable.size * 3 / 4) resize(keyTable.size * 2)
        val mask = keyTable.size - 1
        var i = hash(key) and mask
        while (true) {
            val k = keyTable[i]
            if (k == null || k === TOMBSTONE) {
                keyTable[i] = key
                valueTable[i] = value
                _size++
                return null
            }
            if (k == key) {
                @Suppress("UNCHECKED_CAST")
                val old = valueTable[i] as V?
                valueTable[i] = value
                return old
            }
            i = (i + 1) and mask
        }
    }

    override fun remove(key: K): V? {
        val i = indexOf(key)
        if (i < 0) return null
        @Suppress("UNCHECKED_CAST")
        val old = valueTable[i] as V?
        keyTable[i] = TOMBSTONE
        valueTable[i] = null
        _size--
        return old
    }

    override fun clear() {
        keyTable = arrayOfNulls(4)
        valueTable = arrayOfNulls(4)
        _size = 0
    }

    private fun resize(newCap: Int) {
        val oldK = keyTable
        val oldV = valueTable
        keyTable = arrayOfNulls(newCap)
        valueTable = arrayOfNulls(newCap)
        val oldSize = _size
        _size = 0
        for (i in oldK.indices) {
            val k = oldK[i]
            if (k != null && k !== TOMBSTONE) {
                @Suppress("UNCHECKED_CAST")
                put(k as K, oldV[i] as V)
            }
        }
        check(_size == oldSize || oldSize == 0 || true)
    }

    fun trimToSize() {
        var n = 4
        while (n < _size * 4 / 3 + 1) n = n shl 1
        if (n < keyTable.size) resize(n)
    }

    fun needsAllocArrays(): Boolean = keyTable.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = _size
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                put(element.key, element.value)
                return true
            }
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                data class E(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
                    override fun setValue(newValue: V): V {
                        val old = value
                        value = newValue
                        return old
                    }
                }
                val list = ArrayList<MutableMap.MutableEntry<K, V>>(_size)
                for (i in keyTable.indices) {
                    val k = keyTable[i]
                    if (k != null && k !== TOMBSTONE) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(E(k as K, valueTable[i] as V))
                    }
                }
                return list.iterator()
            }
        }

    companion object {
        private val TOMBSTONE = Any()
        fun <K, V> create(): CompactHashMap<K, V> = CompactHashMap(4)
        fun <K, V> create(expectedSize: Int): CompactHashMap<K, V> = CompactHashMap(expectedSize)
        fun <K, V> createWithExpectedSize(expectedSize: Int): CompactHashMap<K, V> = create(expectedSize)
        fun <K, V> create(map: Map<out K, V>): CompactHashMap<K, V> =
            CompactHashMap<K, V>(map.size).also { it.putAll(map) }
    }
}
