package com.bernaferrari.guavakt.collect

interface MapDifference<K, V> {
    fun areEqual(): Boolean
    fun entriesOnlyOnLeft(): Map<K, V>
    fun entriesOnlyOnRight(): Map<K, V>
    fun entriesInCommon(): Map<K, V>
    fun entriesDiffering(): Map<K, ValueDifference<V>>

    interface ValueDifference<V> {
        fun leftValue(): V
        fun rightValue(): V
    }
}

internal class MapDifferenceImpl<K, V>(
    private val onlyOnLeft: Map<K, V>,
    private val onlyOnRight: Map<K, V>,
    private val onBoth: Map<K, V>,
    private val differing: Map<K, MapDifference.ValueDifference<V>>,
) : MapDifference<K, V> {
    override fun areEqual(): Boolean =
        onlyOnLeft.isEmpty() && onlyOnRight.isEmpty() && differing.isEmpty()
    override fun entriesOnlyOnLeft(): Map<K, V> = onlyOnLeft
    override fun entriesOnlyOnRight(): Map<K, V> = onlyOnRight
    override fun entriesInCommon(): Map<K, V> = onBoth
    override fun entriesDiffering(): Map<K, MapDifference.ValueDifference<V>> = differing

    companion object {
        fun <K, V> compute(left: Map<out K, V>, right: Map<out K, *>): MapDifference<K, V> {
            val onlyLeft = LinkedHashMap<K, V>()
            val onlyRight = LinkedHashMap<K, V>()
            val onBoth = LinkedHashMap<K, V>()
            val differing = LinkedHashMap<K, MapDifference.ValueDifference<V>>()
            for ((k, lv) in left) {
                if (!right.containsKey(k)) onlyLeft[k] = lv
                else {
                    @Suppress("UNCHECKED_CAST")
                    val rv = right[k] as V
                    if (lv == rv) onBoth[k] = lv
                    else differing[k] = object : MapDifference.ValueDifference<V> {
                        override fun leftValue(): V = lv
                        override fun rightValue(): V = rv
                    }
                }
            }
            for ((k, rv) in right) {
                if (!left.containsKey(k)) {
                    @Suppress("UNCHECKED_CAST")
                    onlyRight[k] = rv as V
                }
            }
            return MapDifferenceImpl(onlyLeft, onlyRight, onBoth, differing)
        }
    }
}

/** Comparator-ordered snapshot used by Kotlin's `Maps.difference(..., comparator)` overload. */
internal class SortedMapDifferenceImpl<K, V>(
    onlyOnLeft: Map<K, V>,
    onlyOnRight: Map<K, V>,
    onBoth: Map<K, V>,
    differing: Map<K, MapDifference.ValueDifference<V>>,
) : SortedMapDifference<K, V> {
    private val onlyOnLeft = OrderedMapSnapshot(onlyOnLeft)
    private val onlyOnRight = OrderedMapSnapshot(onlyOnRight)
    private val onBoth = OrderedMapSnapshot(onBoth)
    private val differing = OrderedMapSnapshot(differing)

    override fun areEqual(): Boolean =
        onlyOnLeft.isEmpty() && onlyOnRight.isEmpty() && differing.isEmpty()

    override fun entriesOnlyOnLeft(): Map<K, V> = onlyOnLeft
    override fun entriesOnlyOnRight(): Map<K, V> = onlyOnRight
    override fun entriesInCommon(): Map<K, V> = onBoth
    override fun entriesDiffering(): Map<K, MapDifference.ValueDifference<V>> = differing

    companion object {
        fun <K, V> compute(
            left: Map<out K, V>,
            right: Map<out K, *>,
            comparator: Comparator<in K>,
        ): SortedMapDifference<K, V> {
            val sortedLeft = ComparatorTreeMap<K, V>(comparator)
            val sortedRight = ComparatorTreeMap<K, V>(comparator)
            left.forEach { (key, value) -> sortedLeft[key] = value }
            right.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                run { sortedRight[key] = value as V }
            }

            val onlyOnLeft = ComparatorTreeMap<K, V>(comparator)
            val onlyOnRight = ComparatorTreeMap<K, V>(comparator)
            val onBoth = ComparatorTreeMap<K, V>(comparator)
            val differing = ComparatorTreeMap<K, MapDifference.ValueDifference<V>>(comparator)

            for ((key, leftValue) in sortedLeft) {
                if (!sortedRight.containsKey(key)) {
                    onlyOnLeft[key] = leftValue
                    continue
                }
                @Suppress("UNCHECKED_CAST")
                val rightValue = sortedRight.remove(key) as V
                if (leftValue == rightValue) {
                    onBoth[key] = leftValue
                } else {
                    differing[key] = valueDifference(leftValue, rightValue)
                }
            }
            onlyOnRight.putAll(sortedRight)
            return SortedMapDifferenceImpl(onlyOnLeft, onlyOnRight, onBoth, differing)
        }

        private fun <V> valueDifference(left: V, right: V): MapDifference.ValueDifference<V> =
            object : MapDifference.ValueDifference<V> {
                override fun leftValue(): V = left
                override fun rightValue(): V = right
                override fun toString(): String = "($left, $right)"
            }
    }
}

/** Hides the mutable linked-map implementation while retaining comparator encounter order. */
private class OrderedMapSnapshot<K, V>(source: Map<out K, V>) : Map<K, V> {
    private val entriesByKey = LinkedHashMap<K, V>(source)

    override val entries: Set<Map.Entry<K, V>>
        get() = entriesByKey.entries.map { (key, value) ->
            object : Map.Entry<K, V> {
                override val key: K = key
                override val value: V = value
            }
        }.toSet()
    override val keys: Set<K> get() = entriesByKey.keys.toSet()
    override val size: Int get() = entriesByKey.size
    override val values: Collection<V> get() = entriesByKey.values.toList()
    override fun containsKey(key: K): Boolean = entriesByKey.containsKey(key)
    override fun containsValue(value: V): Boolean = entriesByKey.containsValue(value)
    override fun get(key: K): V? = entriesByKey[key]
    override fun isEmpty(): Boolean = entriesByKey.isEmpty()
}
