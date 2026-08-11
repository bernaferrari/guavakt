package dev.guavakt.collect

internal class ReverseOrdering<T>(private val forward: Ordering<in T>) : Ordering<T>() {
    override fun compare(left: T, right: T): Int = forward.compare(right, left)
}
