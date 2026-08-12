package com.bernaferrari.guavakt.collect

interface Multiset<E> : Collection<E> {
    fun count(element: Any?): Int
    fun add(element: E, occurrences: Int): Int
    fun remove(element: Any?, occurrences: Int): Int
    fun setCount(element: E, count: Int): Int
    fun setCount(element: E, oldCount: Int, newCount: Int): Boolean {
        require(oldCount >= 0) { "oldCount cannot be negative: $oldCount" }
        require(newCount >= 0) { "newCount cannot be negative: $newCount" }
        if (count(element) != oldCount) return false
        setCount(element, newCount)
        return true
    }
    fun elementSet(): Set<E>
    fun entrySet(): Set<Entry<E>>
    interface Entry<E> {
        fun getElement(): E
        fun getCount(): Int
    }
}
