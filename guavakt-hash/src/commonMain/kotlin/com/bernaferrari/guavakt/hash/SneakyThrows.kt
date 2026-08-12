package com.bernaferrari.guavakt.hash

internal object SneakyThrows {
    fun <T> sneakyThrow(t: Throwable): T { throw t }
}
