package dev.guavakt.collect

object Interners {
    fun <E> newStrongInterner(): Interner<E> = StrongInterner()

    fun <E> newWeakInterner(): Interner<E> = StrongInterner() // KMP: strong only (no weak refs)

    fun newBuilder(): InternerBuilder = InternerBuilder()

    class InternerBuilder {
        private var strong = true
        fun strong(): InternerBuilder = apply { strong = true }
        fun weak(): InternerBuilder = apply { strong = false } // still strong on KMP
        fun <E> build(): Interner<E> = StrongInterner()
    }

    private class StrongInterner<E> : Interner<E> {
        private val map = LinkedHashMap<E, E>()
        override fun intern(sample: E): E = map.getOrPut(sample) { sample }
    }
}
