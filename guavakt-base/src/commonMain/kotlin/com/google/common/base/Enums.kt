package dev.guavakt.base

object Enums {
    fun <T : Enum<T>> getIfPresent(enumClass: Any, value: String): Optional<T> {
        // KMP: no Class<T>; callers pass enum entries via string match on name is app-specific.
        // Provide API surface; without reflection we cannot resolve — return absent.
        @Suppress("UNCHECKED_CAST")
        return Optional.absent()
    }
    fun stringConverter(enumClass: Any): Converter<String, Any> =
        object : Converter<String, Any>() {
            override fun doForward(a: String): Any = a
            override fun doBackward(b: Any): String = b.toString()
        }
}
