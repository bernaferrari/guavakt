package com.bernaferrari.guavakt.base

fun interface Function<F, T> {
    fun apply(input: F): T
}
