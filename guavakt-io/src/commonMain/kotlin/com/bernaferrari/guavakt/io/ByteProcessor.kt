package com.bernaferrari.guavakt.io

/** Incrementally consumes byte chunks; returning `false` stops the enclosing read. */
interface ByteProcessor<T> {
    fun processBytes(buffer: ByteArray, offset: Int, length: Int): Boolean
    fun getResult(): T
}
