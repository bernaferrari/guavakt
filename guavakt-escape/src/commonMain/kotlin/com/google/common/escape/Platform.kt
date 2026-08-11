package dev.guavakt.escape

internal object Platform {
    fun charBufferFromThreadLocal(): CharArray = CharArray(1024)
}
