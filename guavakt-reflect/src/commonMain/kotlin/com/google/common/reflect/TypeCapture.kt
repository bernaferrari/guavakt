package dev.guavakt.reflect

/**
 * Guava TypeCapture — captures generic type argument via subclassing (super-type-token pattern).
 * On KMP, stores an optional explicit type name since reified erasure differs by platform.
 */
abstract class TypeCapture<T> {
    protected open fun capture(): String =
        platformClassDisplayName(this::class)
}
