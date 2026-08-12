package com.bernaferrari.guavakt.base

fun interface Predicate<T> {
    fun apply(input: T): Boolean
}
