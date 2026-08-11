package dev.guavakt.base

/** Guava PairwiseEquivalence — element-wise iterable equivalence. */
class PairwiseEquivalence<T>(private val elementEquivalence: Equivalence<in T>) :
    Equivalence<Iterable<T>>() {
    override fun doEquivalent(a: Iterable<T>, b: Iterable<T>): Boolean {
        val ia = a.iterator()
        val ib = b.iterator()
        while (ia.hasNext() && ib.hasNext()) {
            if (!elementEquivalence.equivalent(ia.next(), ib.next())) return false
        }
        return !ia.hasNext() && !ib.hasNext()
    }
    override fun doHash(t: Iterable<T>): Int {
        var result = 78721
        for (element in t) result = result * 24943 + elementEquivalence.hash(element)
        return result
    }

    override fun equals(other: Any?): Boolean =
        other is PairwiseEquivalence<*> && elementEquivalence == other.elementEquivalence

    override fun hashCode(): Int = elementEquivalence.hashCode() xor 0x46a3eb07

    override fun toString(): String = "$elementEquivalence.pairwise()"
}
