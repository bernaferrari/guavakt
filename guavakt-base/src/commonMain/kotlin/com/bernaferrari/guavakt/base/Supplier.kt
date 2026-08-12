package com.bernaferrari.guavakt.base

fun interface Supplier<out T> {
    fun get(): T
}
