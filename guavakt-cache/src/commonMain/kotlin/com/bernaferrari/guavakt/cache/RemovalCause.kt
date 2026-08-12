package com.bernaferrari.guavakt.cache

enum class RemovalCause {
    EXPLICIT { override fun wasEvicted() = false },
    REPLACED { override fun wasEvicted() = false },
    COLLECTED { override fun wasEvicted() = true },
    EXPIRED { override fun wasEvicted() = true },
    SIZE { override fun wasEvicted() = true };
    abstract fun wasEvicted(): Boolean
    fun wireName(): String = name
}
