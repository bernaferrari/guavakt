package com.bernaferrari.guavakt.collect

fun interface Interner<E> {
    fun intern(sample: E): E
}
