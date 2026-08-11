package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible

/** Guava Objects — prefer Kotlin `==` / `hashCode()` in new code. */
@GwtCompatible
object Objects {
    fun equal(a: Any?, b: Any?): Boolean = a == b

    fun hashCode(vararg objects: Any?): Int {
        var result = 1
        for (element in objects) {
            result = 31 * result + (element?.hashCode() ?: 0)
        }
        return result
    }
}
