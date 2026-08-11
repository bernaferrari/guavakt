package dev.guavakt.collect

open class Hashing {
    open fun isEmpty(): Boolean = true
    open fun size(): Int = 0
    companion object {
        fun create(): Hashing = Hashing()
    }
}
