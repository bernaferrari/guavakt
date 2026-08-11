package dev.guavakt.hash

/** Portable standard funnels with stable equality for BloomFilter compatibility. */
object Funnels {
    private object ByteArrayFunnel : Funnel<ByteArray> {
        override fun funnel(from: ByteArray, into: PrimitiveSink) { into.putBytes(from) }
        override fun toString(): String = "Funnels.byteArrayFunnel()"
    }

    private object UnencodedCharsFunnel : Funnel<CharSequence> {
        override fun funnel(from: CharSequence, into: PrimitiveSink) { into.putUnencodedChars(from) }
        override fun toString(): String = "Funnels.unencodedCharsFunnel()"
    }

    private object IntegerFunnel : Funnel<Int> {
        override fun funnel(from: Int, into: PrimitiveSink) { into.putInt(from) }
        override fun toString(): String = "Funnels.integerFunnel()"
    }

    private object LongFunnel : Funnel<Long> {
        override fun funnel(from: Long, into: PrimitiveSink) { into.putLong(from) }
        override fun toString(): String = "Funnels.longFunnel()"
    }

    fun byteArrayFunnel(): Funnel<ByteArray> = ByteArrayFunnel
    fun unencodedCharsFunnel(): Funnel<CharSequence> = UnencodedCharsFunnel

    /** Early GuavaKt alias for [unencodedCharsFunnel]. */
    fun stringFunnel(): Funnel<CharSequence> = UnencodedCharsFunnel

    fun integerFunnel(): Funnel<Int> = IntegerFunnel
    fun longFunnel(): Funnel<Long> = LongFunnel

    fun <E> sequentialFunnel(elementFunnel: Funnel<E>): Funnel<Iterable<E>> =
        SequentialFunnel(elementFunnel)

    private class SequentialFunnel<E>(private val elementFunnel: Funnel<E>) : Funnel<Iterable<E>> {
        override fun funnel(from: Iterable<E>, into: PrimitiveSink) {
            from.forEach { elementFunnel.funnel(it, into) }
        }

        override fun equals(other: Any?): Boolean =
            other is SequentialFunnel<*> && elementFunnel == other.elementFunnel

        override fun hashCode(): Int = elementFunnel.hashCode()
        override fun toString(): String = "Funnels.sequentialFunnel($elementFunnel)"
    }
}
