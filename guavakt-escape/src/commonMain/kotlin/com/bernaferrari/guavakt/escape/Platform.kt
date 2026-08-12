package com.bernaferrari.guavakt.escape

internal object Platform {
    fun charBufferFromThreadLocal(): CharArray = CharArray(1024)
}
