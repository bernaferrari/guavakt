package dev.guavakt.hash

fun interface Funnel<T> {
    fun funnel(from: T, into: PrimitiveSink)
}
