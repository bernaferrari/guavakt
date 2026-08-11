package dev.guavakt.io

enum class RecursiveDeleteOption {
    INSTANCE;
    fun wireName(): String = name
}
