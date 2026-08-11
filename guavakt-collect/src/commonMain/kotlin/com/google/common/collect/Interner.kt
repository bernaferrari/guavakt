package dev.guavakt.collect

fun interface Interner<E> {
    fun intern(sample: E): E
}
