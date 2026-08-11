package dev.guavakt.parity

import com.google.common.collect.Range as GuavaRange
import com.google.common.collect.TreeRangeMap as GuavaTreeRangeMap
import com.google.common.collect.TreeRangeSet as GuavaTreeRangeSet
import dev.guavakt.collect.Range
import dev.guavakt.collect.TreeRangeMap
import dev.guavakt.collect.TreeRangeSet
import dev.guavakt.math.BigInteger as GuavaKtBigInteger
import java.math.BigInteger as JavaBigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.random.Random

class RangeDifferentialTest {
    @Test fun rangeSetRemovalPreservesUnboundedRemnants() {
        val guava = GuavaTreeRangeSet.create<Int>().apply {
            add(GuavaRange.all())
            remove(GuavaRange.closed(0, 10))
        }
        val ours = TreeRangeSet.create<Int>().apply {
            add(Range.all())
            remove(Range.closed(0, 10))
        }
        for (point in listOf(Int.MIN_VALUE, -1, 0, 1, 9, 10, 11, Int.MAX_VALUE)) {
            assertEquals(guava.contains(point), ours.contains(point), "point=$point")
        }
    }

    @Test fun rangeSetRemovalPreservesExcludedBoundaryPoints() {
        val ours = TreeRangeSet.create<Int>().apply {
            add(Range.closed(0, 10))
            remove(Range.open(0, 10))
        }
        assertEquals(setOf(Range.singleton(0), Range.singleton(10)), ours.asRanges())
    }

    @Test fun rangeMapRemovalPreservesBothUnboundedSidesAndOpenEndpoints() {
        val guava = GuavaTreeRangeMap.create<Int, String>().apply {
            put(GuavaRange.all(), "value")
            remove(GuavaRange.open(0, 10))
        }
        val ours = TreeRangeMap.create<Int, String>().apply {
            put(Range.all(), "value")
            remove(Range.open(0, 10))
        }
        for (point in listOf(Int.MIN_VALUE, -1, 0, 1, 9, 10, 11, Int.MAX_VALUE)) {
            assertEquals(guava.get(point), ours.get(point), "point=$point")
        }
    }

    @Test fun halfInfiniteRemovalMatchesGuava() {
        val guava = GuavaTreeRangeMap.create<Int, String>().apply {
            put(GuavaRange.all(), "value")
            remove(GuavaRange.lessThan(0))
        }
        val ours = TreeRangeMap.create<Int, String>().apply {
            put(Range.all(), "value")
            remove(Range.lessThan(0))
        }
        for (point in listOf(-1, 0, 1)) assertEquals(guava.get(point), ours.get(point))
    }

    @Test fun heldComplementAndSubRangeViewsMatchGuavaWriteThroughSemantics() {
        val guava = GuavaTreeRangeSet.create<Int>().apply { add(GuavaRange.closed(1, 3)) }
        val ours = TreeRangeSet.create<Int>().apply { add(Range.closed(1, 3)) }
        val guavaComplement = guava.complement()
        val oursComplement = ours.complement()
        val guavaSubRange = guava.subRangeSet(GuavaRange.closed(2, 6))
        val oursSubRange = ours.subRangeSet(Range.closed(2, 6))

        assertEquals(
            listOf(
                guavaSubRange.complement().asRanges().map(Any::toString),
                guavaSubRange.complement().complement() === guavaSubRange,
            ),
            listOf(
                oursSubRange.complement().asRanges().map(Any::toString),
                oursSubRange.complement().complement() === oursSubRange,
            ),
        )

        fun assertViewsMatch() {
            assertEquals(
                listOf(
                    guava.asRanges().map(Any::toString),
                    guavaComplement.asRanges().map(Any::toString),
                    guavaSubRange.asRanges().map(Any::toString),
                    listOf(-1, 1, 2, 3, 4, 5, 6, 7, 8).map(guava::contains),
                    guavaComplement.complement() === guava,
                ),
                listOf(
                    ours.asRanges().map(Any::toString),
                    oursComplement.asRanges().map(Any::toString),
                    oursSubRange.asRanges().map(Any::toString),
                    listOf(-1, 1, 2, 3, 4, 5, 6, 7, 8).map(ours::contains),
                    oursComplement.complement() === ours,
                ),
            )
        }

        guava.add(GuavaRange.closed(5, 7))
        ours.add(Range.closed(5, 7))
        assertViewsMatch()

        guavaComplement.add(GuavaRange.closed(2, 5))
        oursComplement.add(Range.closed(2, 5))
        assertViewsMatch()

        guavaSubRange.add(GuavaRange.closed(3, 4))
        oursSubRange.add(Range.closed(3, 4))
        guavaSubRange.remove(GuavaRange.closed(3, 6))
        oursSubRange.remove(Range.closed(3, 6))
        assertViewsMatch()

        assertFailsWith<IllegalArgumentException> { guavaSubRange.add(GuavaRange.singleton(7)) }
        assertFailsWith<IllegalArgumentException> { oursSubRange.add(Range.singleton(7)) }
        guavaSubRange.clear()
        oursSubRange.clear()
        assertViewsMatch()
    }

    @Test fun heldAsRangesCollectionsMatchGuavaLivenessOrderingAndRemoval() {
        val guava = GuavaTreeRangeSet.create<Int>().apply { add(GuavaRange.closedOpen(1, 3)) }
        val ours = TreeRangeSet.create<Int>().apply { add(Range.closedOpen(1, 3)) }
        val guavaAscending = guava.asRanges()
        val oursAscending = ours.asRanges()
        val guavaDescending = guava.asDescendingSetOfRanges()
        val oursDescending = ours.asDescendingSetOfRanges()

        fun asStrings(ranges: Iterable<*>): List<String> = ranges.map { it.toString() }
        fun assertViewsMatch() {
            assertEquals(
                listOf(
                    asStrings(guava.asRanges()),
                    asStrings(guavaAscending),
                    asStrings(guavaDescending),
                ),
                listOf(
                    asStrings(ours.asRanges()),
                    asStrings(oursAscending),
                    asStrings(oursDescending),
                ),
            )
        }

        guava.add(GuavaRange.closed(5, 7))
        ours.add(Range.closed(5, 7))
        assertViewsMatch()

        assertEquals(
            guavaAscending.remove(GuavaRange.closedOpen(1, 3)),
            oursAscending.remove(Range.closedOpen(1, 3)),
        )
        assertViewsMatch()

        guavaDescending.iterator().also { iterator ->
            iterator.next()
            iterator.remove()
        }
        oursDescending.iterator().also { iterator ->
            iterator.next()
            iterator.remove()
        }
        assertViewsMatch()

        assertFailsWith<UnsupportedOperationException> { guavaAscending.add(GuavaRange.singleton(9)) }
        assertFailsWith<UnsupportedOperationException> { oursAscending.add(Range.singleton(9)) }

        guava.add(GuavaRange.closed(1, 3))
        ours.add(Range.closed(1, 3))
        val guavaComplementRanges = guava.complement().asRanges()
        val oursComplementRanges = ours.complement().asRanges()
        assertFailsWith<UnsupportedOperationException> { guavaComplementRanges.remove(GuavaRange.lessThan(1)) }
        assertFalse(oursComplementRanges is MutableSet<*>)
        guava.add(GuavaRange.lessThan(1))
        ours.add(Range.lessThan(1))
        assertEquals(asStrings(guavaComplementRanges), asStrings(oursComplementRanges))
        assertViewsMatch()

        val guavaSubRanges = guava.subRangeSet(GuavaRange.closed(2, 6)).asRanges()
        val oursSubRanges = ours.subRangeSet(Range.closed(2, 6)).asRanges()
        assertFailsWith<UnsupportedOperationException> { guavaSubRanges.remove(GuavaRange.closed(2, 3)) }
        assertFalse(oursSubRanges is MutableSet<*>)
        guava.add(GuavaRange.closed(5, 7))
        ours.add(Range.closed(5, 7))
        assertEquals(asStrings(guavaSubRanges), asStrings(oursSubRanges))
        assertViewsMatch()
    }

    @Test fun heldSubRangeMapAndMapOfRangesViewsMatchGuava() {
        val guava = GuavaTreeRangeMap.create<Int, String>().apply {
            put(GuavaRange.closed(1, 3), "a")
            put(GuavaRange.closed(5, 7), "b")
        }
        val ours = TreeRangeMap.create<Int, String>().apply {
            put(Range.closed(1, 3), "a")
            put(Range.closed(5, 7), "b")
        }
        val guavaRanges = guava.asMapOfRanges()
        val oursRanges = ours.asMapOfRanges()
        val guavaDescending = guava.asDescendingMapOfRanges()
        val oursDescending = ours.asDescendingMapOfRanges()
        val guavaSubRange = guava.subRangeMap(GuavaRange.closed(2, 6))
        val oursSubRange = ours.subRangeMap(Range.closed(2, 6))
        val guavaSubRanges = guavaSubRange.asMapOfRanges()
        @Suppress("UNCHECKED_CAST")
        val oursSubRanges = oursSubRange.asMapOfRanges() as MutableMap<Range<Int>, String>

        fun entries(map: Map<*, *>): List<String> = map.entries.map { "${it.key}=${it.value}" }
        fun assertViewsMatch() {
            assertEquals(
                listOf(
                    entries(guava.asMapOfRanges()),
                    entries(guavaRanges),
                    entries(guavaDescending),
                    entries(guavaSubRanges),
                    guava.asMapOfRanges() === guava.asMapOfRanges(),
                    guavaSubRange.asMapOfRanges() === guavaSubRange.asMapOfRanges(),
                ),
                listOf(
                    entries(ours.asMapOfRanges()),
                    entries(oursRanges),
                    entries(oursDescending),
                    entries(oursSubRanges),
                    ours.asMapOfRanges() === ours.asMapOfRanges(),
                    oursSubRange.asMapOfRanges() === oursSubRange.asMapOfRanges(),
                ),
            )
        }

        assertViewsMatch()
        assertEquals(
            listOf(
                guavaSubRange.span().toString(),
                guavaSubRange.getEntry(2)?.let { it.key.toString() to it.value },
                entries(guavaSubRange.subRangeMap(GuavaRange.closed(3, 5)).asMapOfRanges()),
            ),
            listOf(
                oursSubRange.span().toString(),
                oursSubRange.getEntry(2)?.let { it.key.toString() to it.value },
                entries(oursSubRange.subRangeMap(Range.closed(3, 5)).asMapOfRanges()),
            ),
        )
        val guavaEmptyNested = guavaSubRange.subRangeMap(GuavaRange.closed(8, 9))
        val oursEmptyNested = oursSubRange.subRangeMap(Range.closed(8, 9))
        assertEquals(entries(guavaEmptyNested.asMapOfRanges()), entries(oursEmptyNested.asMapOfRanges()))
        assertFailsWith<IllegalArgumentException> { guavaEmptyNested.put(GuavaRange.singleton(8), "outside") }
        assertFailsWith<IllegalArgumentException> { oursEmptyNested.put(Range.singleton(8), "outside") }

        guava.put(GuavaRange.closed(9, 10), "c")
        ours.put(Range.closed(9, 10), "c")
        assertViewsMatch()

        guavaSubRange.putCoalescing(GuavaRange.closed(3, 4), "a")
        oursSubRange.putCoalescing(Range.closed(3, 4), "a")
        assertViewsMatch()
        guavaSubRange.remove(GuavaRange.closed(3, 6))
        oursSubRange.remove(Range.closed(3, 6))
        assertViewsMatch()

        assertFailsWith<IllegalArgumentException> { guavaSubRange.put(GuavaRange.singleton(7), "outside") }
        assertFailsWith<IllegalArgumentException> { oursSubRange.put(Range.singleton(7), "outside") }
        assertEquals(
            guavaRanges.remove(GuavaRange.closedOpen(1, 3)),
            oursRanges.remove(Range.closedOpen(1, 3)),
        )
        assertViewsMatch()

        guavaDescending.entries.iterator().also { iterator ->
            iterator.next()
            iterator.remove()
        }
        oursDescending.entries.iterator().also { iterator ->
            iterator.next()
            iterator.remove()
        }
        assertViewsMatch()

        assertFailsWith<UnsupportedOperationException> { guavaRanges[GuavaRange.singleton(11)] = "nope" }
        assertFailsWith<UnsupportedOperationException> { oursRanges[Range.singleton(11)] = "nope" }

        guavaSubRanges.clear()
        oursSubRanges.clear()
        assertViewsMatch()
    }

    @Test fun comparatorEquivalentEndpointsUseGuavaCutSemantics() {
        val first = ComparatorAlias(4, "first")
        val second = ComparatorAlias(4, "second")
        val third = ComparatorAlias(4, "third")
        val fourth = ComparatorAlias(4, "fourth")

        val guavaClosedOpen = GuavaRange.closedOpen(first, second)
        val oursClosedOpen = Range.closedOpen(first, second)
        val guavaOpenClosed = GuavaRange.openClosed(first, second)
        val oursOpenClosed = Range.openClosed(first, second)

        assertEquals(
            listOf(
                guavaClosedOpen.isEmpty,
                guavaOpenClosed.isEmpty,
                guavaClosedOpen == GuavaRange.closedOpen(third, fourth),
                guavaClosedOpen.contains(first),
            ),
            listOf(
                oursClosedOpen.isEmpty(),
                oursOpenClosed.isEmpty(),
                oursClosedOpen == Range.closedOpen(third, fourth),
                oursClosedOpen.contains(first),
            ),
        )
        assertFailsWith<IllegalArgumentException> { GuavaRange.open(first, second) }
        assertFailsWith<IllegalArgumentException> { Range.open(first, second) }

        val guavaSet = GuavaTreeRangeSet.create<ComparatorAlias>().apply { add(guavaClosedOpen) }
        val oursSet = TreeRangeSet.create<ComparatorAlias>().apply { add(oursClosedOpen) }
        val guavaMap = GuavaTreeRangeMap.create<ComparatorAlias, String>().apply { put(guavaOpenClosed, "empty") }
        val oursMap = TreeRangeMap.create<ComparatorAlias, String>().apply { put(oursOpenClosed, "empty") }
        assertEquals(guavaSet.asRanges().map(Any::toString), oursSet.asRanges().map(Any::toString))
        assertEquals(
            guavaMap.asMapOfRanges().entries.map { "${it.key}=${it.value}" },
            oursMap.asMapOfRanges().entries.map { "${it.key}=${it.value}" },
        )

        val guavaFiniteHashes = listOf(
            GuavaRange.closed(1, 2).hashCode(),
            GuavaRange.open(1, 2).hashCode(),
            GuavaRange.closedOpen(1, 2).hashCode(),
            GuavaRange.openClosed(1, 2).hashCode(),
        )
        val oursFiniteHashes = listOf(
            Range.closed(1, 2).hashCode(),
            Range.open(1, 2).hashCode(),
            Range.closedOpen(1, 2).hashCode(),
            Range.openClosed(1, 2).hashCode(),
        )
        assertEquals(guavaFiniteHashes, oursFiniteHashes)
    }

    @Test fun derivedRangeViewIteratorsMatchGuavaFailFastBehavior() {
        val guavaSet = GuavaTreeRangeSet.create<Int>().apply {
            add(GuavaRange.closed(1, 2))
            add(GuavaRange.closed(4, 5))
        }
        val oursSet = TreeRangeSet.create<Int>().apply {
            add(Range.closed(1, 2))
            add(Range.closed(4, 5))
        }
        val guavaSetIterator = guavaSet.subRangeSet(GuavaRange.atLeast(0)).asRanges().iterator()
        val oursSetIterator = oursSet.subRangeSet(Range.atLeast(0)).asRanges().iterator()
        guavaSetIterator.next()
        oursSetIterator.next()
        guavaSet.add(GuavaRange.closed(8, 9))
        oursSet.add(Range.closed(8, 9))
        assertEquals(
            iteratorFailureName { guavaSetIterator.hasNext() },
            iteratorFailureName { oursSetIterator.hasNext() },
            "subRangeSet.asRanges iterator",
        )

        val guavaMap = GuavaTreeRangeMap.create<Int, String>().apply {
            put(GuavaRange.closed(1, 2), "a")
            put(GuavaRange.closed(4, 5), "b")
        }
        val oursMap = TreeRangeMap.create<Int, String>().apply {
            put(Range.closed(1, 2), "a")
            put(Range.closed(4, 5), "b")
        }
        val guavaMapIterator = guavaMap.subRangeMap(GuavaRange.atLeast(0)).asMapOfRanges().entries.iterator()
        val oursMapIterator = oursMap.subRangeMap(Range.atLeast(0)).asMapOfRanges().entries.iterator()
        guavaMapIterator.next()
        oursMapIterator.next()
        guavaMap.put(GuavaRange.closed(8, 9), "c")
        oursMap.put(Range.closed(8, 9), "c")
        assertEquals(
            iteratorFailureName { guavaMapIterator.hasNext() },
            iteratorFailureName { oursMapIterator.hasNext() },
            "subRangeMap.asMapOfRanges iterator",
        )
    }

    @Test fun randomizedExoticCutRangeSetTracesMatchGuava() {
        repeat(16) { seed ->
            val random = Random(seed)
            val guava = GuavaTreeRangeSet.create<Int>()
            val ours = TreeRangeSet.create<Int>()
            val guavaComplement = guava.complement()
            val oursComplement = ours.complement()

            repeat(256) { step ->
                val range = randomRange(random)
                val operation = when (random.nextInt(4)) {
                    0 -> "add"
                    1 -> "remove"
                    2 -> "complement.add"
                    else -> "complement.remove"
                }
                when (operation) {
                    "add" -> {
                        guava.add(range.guava)
                        ours.add(range.ours)
                    }
                    "remove" -> {
                        guava.remove(range.guava)
                        ours.remove(range.ours)
                    }
                    "complement.add" -> {
                        guavaComplement.add(range.guava)
                        oursComplement.add(range.ours)
                    }
                    else -> {
                        guavaComplement.remove(range.guava)
                        oursComplement.remove(range.ours)
                    }
                }

                val context = "seed=$seed step=$step operation=$operation range=${range.ours}"
                assertEquals(guava.asRanges().map(Any::toString), ours.asRanges().map(Any::toString), context)
                assertEquals(
                    guavaComplement.asRanges().map(Any::toString),
                    oursComplement.asRanges().map(Any::toString),
                    "$context complement",
                )
                for (point in tracePoints) {
                    assertEquals(guava.contains(point), ours.contains(point), "$context point=$point")
                }

                val view = randomRange(random)
                assertEquals(
                    guava.subRangeSet(view.guava).asRanges().map(Any::toString),
                    ours.subRangeSet(view.ours).asRanges().map(Any::toString),
                    "$context subRangeSet=$view",
                )
                assertEquals(guava.encloses(view.guava), ours.encloses(view.ours), "$context encloses")
                assertEquals(guava.intersects(view.guava), ours.intersects(view.ours), "$context intersects")
            }
        }
    }

    /**
     * Replays arbitrary-precision RangeSet mutations against Guava. The property-controlled
     * counts are shared with the numeric fuzzing test, so an extended run exercises both layers
     * with one reproducible command.
     */
    @Test fun randomizedArbitraryPrecisionRangeSetTracesMatchGuava() {
        val seeds = System.getProperty("guavakt.fuzz.seeds", "12").toInt()
        val casesPerSeed = System.getProperty("guavakt.fuzz.cases", "96").toInt()
        repeat(seeds) { seed ->
            val random = Random(seed + 20_000)
            val guava = GuavaTreeRangeSet.create<JavaBigInteger>()
            val ours = TreeRangeSet.create<GuavaKtBigInteger>()
            val guavaComplement = guava.complement()
            val oursComplement = ours.complement()

            repeat(casesPerSeed) { step ->
                val range = randomBigIntegerRange(random)
                val operation = when (random.nextInt(4)) {
                    0 -> "add"
                    1 -> "remove"
                    2 -> "complement.add"
                    else -> "complement.remove"
                }
                when (operation) {
                    "add" -> {
                        guava.add(range.guava)
                        ours.add(range.ours)
                    }
                    "remove" -> {
                        guava.remove(range.guava)
                        ours.remove(range.ours)
                    }
                    "complement.add" -> {
                        guavaComplement.add(range.guava)
                        oursComplement.add(range.ours)
                    }
                    else -> {
                        guavaComplement.remove(range.guava)
                        oursComplement.remove(range.ours)
                    }
                }

                val context = "seed=$seed step=$step operation=$operation range=${range.ours}"
                assertEquals(guava.asRanges().map(Any::toString), ours.asRanges().map(Any::toString), context)
                assertEquals(
                    guavaComplement.asRanges().map(Any::toString),
                    oursComplement.asRanges().map(Any::toString),
                    "$context complement",
                )
                for (point in range.probes) {
                    assertEquals(guava.contains(point.java), ours.contains(point.kotlin), "$context point=${point.kotlin}")
                }

                val view = randomBigIntegerRange(random)
                assertEquals(
                    guava.subRangeSet(view.guava).asRanges().map(Any::toString),
                    ours.subRangeSet(view.ours).asRanges().map(Any::toString),
                    "$context subRangeSet=${view.ours}",
                )
                assertEquals(guava.encloses(view.guava), ours.encloses(view.ours), "$context encloses")
                assertEquals(guava.intersects(view.guava), ours.intersects(view.ours), "$context intersects")
            }
        }
    }

    @Test fun randomizedExoticCutRangeMapTracesMatchGuava() {
        repeat(16) { seed ->
            val random = Random(seed + 1_000)
            val guava = GuavaTreeRangeMap.create<Int, Int>()
            val ours = TreeRangeMap.create<Int, Int>()
            val history = ArrayList<String>()

            repeat(256) { step ->
                val range = randomRange(random)
                val value = random.nextInt(3)
                val operation = when (random.nextInt(3)) {
                    0 -> "put"
                    1 -> "putCoalescing"
                    else -> "remove"
                }
                when (operation) {
                    "put" -> {
                        guava.put(range.guava, value)
                        ours.put(range.ours, value)
                    }
                    "putCoalescing" -> {
                        guava.putCoalescing(range.guava, value)
                        ours.putCoalescing(range.ours, value)
                    }
                    else -> {
                        guava.remove(range.guava)
                        ours.remove(range.ours)
                    }
                }

                history.add("$step:$operation(${range.ours}, $value)")
                val context =
                    "seed=$seed step=$step operation=$operation range=${range.ours} value=$value " +
                        "recent=${history.takeLast(12)}"
                assertEquals(
                    guava.asMapOfRanges().entries.map { "${it.key}=${it.value}" },
                    ours.asMapOfRanges().entries.map { "${it.key}=${it.value}" },
                    context,
                )
                for (point in tracePoints) {
                    assertEquals(guava[point], ours[point], "$context point=$point")
                }

                val view = randomRange(random)
                assertEquals(
                    guava.subRangeMap(view.guava).asMapOfRanges().entries.map { "${it.key}=${it.value}" },
                    ours.subRangeMap(view.ours).asMapOfRanges().entries.map { "${it.key}=${it.value}" },
                    "$context subRangeMap=$view",
                )
            }
        }
    }

    private data class IntRangePair(
        val guava: GuavaRange<Int>,
        val ours: Range<Int>,
    )

    private data class BigIntegerRangePair(
        val guava: GuavaRange<JavaBigInteger>,
        val ours: Range<GuavaKtBigInteger>,
        val probes: List<BigIntegerProbe>,
    )

    private data class BigIntegerProbe(
        val java: JavaBigInteger,
        val kotlin: GuavaKtBigInteger,
    )

    private data class ComparatorAlias(
        val bucket: Int,
        val label: String,
    ) : Comparable<ComparatorAlias> {
        override fun compareTo(other: ComparatorAlias): Int = bucket.compareTo(other.bucket)
    }

    private fun randomRange(random: Random): IntRangePair {
        val first = randomEndpoint(random)
        val second = randomEndpoint(random)
        val lower = minOf(first, second)
        val upper = maxOf(first, second)
        return when (random.nextInt(9)) {
            0 -> IntRangePair(GuavaRange.all(), Range.all())
            1 -> IntRangePair(GuavaRange.lessThan(first), Range.lessThan(first))
            2 -> IntRangePair(GuavaRange.atMost(first), Range.atMost(first))
            3 -> IntRangePair(GuavaRange.greaterThan(first), Range.greaterThan(first))
            4 -> IntRangePair(GuavaRange.atLeast(first), Range.atLeast(first))
            5 -> if (lower == upper) {
                IntRangePair(GuavaRange.closedOpen(lower, upper), Range.closedOpen(lower, upper))
            } else {
                IntRangePair(GuavaRange.open(lower, upper), Range.open(lower, upper))
            }
            6 -> IntRangePair(GuavaRange.closed(lower, upper), Range.closed(lower, upper))
            7 -> IntRangePair(GuavaRange.closedOpen(lower, upper), Range.closedOpen(lower, upper))
            else -> IntRangePair(GuavaRange.openClosed(lower, upper), Range.openClosed(lower, upper))
        }
    }

    private fun randomBigIntegerRange(random: Random): BigIntegerRangePair {
        val firstSource = randomBigIntegerEndpoint(random)
        val secondSource = randomBigIntegerEndpoint(random)
        val firstJava = JavaBigInteger(firstSource)
        val secondJava = JavaBigInteger(secondSource)
        val firstKotlin = GuavaKtBigInteger.parse(firstSource)
        val secondKotlin = GuavaKtBigInteger.parse(secondSource)
        val lowerJava = minOf(firstJava, secondJava)
        val upperJava = maxOf(firstJava, secondJava)
        val lowerKotlin = minOf(firstKotlin, secondKotlin)
        val upperKotlin = maxOf(firstKotlin, secondKotlin)
        val ranges = when (random.nextInt(9)) {
            0 -> GuavaRange.all<JavaBigInteger>() to Range.all<GuavaKtBigInteger>()
            1 -> GuavaRange.lessThan(firstJava) to Range.lessThan(firstKotlin)
            2 -> GuavaRange.atMost(firstJava) to Range.atMost(firstKotlin)
            3 -> GuavaRange.greaterThan(firstJava) to Range.greaterThan(firstKotlin)
            4 -> GuavaRange.atLeast(firstJava) to Range.atLeast(firstKotlin)
            5 -> if (lowerJava == upperJava) {
                GuavaRange.closedOpen(lowerJava, upperJava) to Range.closedOpen(lowerKotlin, upperKotlin)
            } else {
                GuavaRange.open(lowerJava, upperJava) to Range.open(lowerKotlin, upperKotlin)
            }
            6 -> GuavaRange.closed(lowerJava, upperJava) to Range.closed(lowerKotlin, upperKotlin)
            7 -> GuavaRange.closedOpen(lowerJava, upperJava) to Range.closedOpen(lowerKotlin, upperKotlin)
            else -> GuavaRange.openClosed(lowerJava, upperJava) to Range.openClosed(lowerKotlin, upperKotlin)
        }
        val probes = listOf(
            firstSource,
            secondSource,
            "-1",
            "0",
            "1",
            "1" + "0".repeat(120),
            "-1" + "0".repeat(120),
        ).distinct().map { BigIntegerProbe(JavaBigInteger(it), GuavaKtBigInteger.parse(it)) }
        return BigIntegerRangePair(ranges.first, ranges.second, probes)
    }

    private fun randomBigIntegerEndpoint(random: Random): String = when (random.nextInt(10)) {
        0 -> "0"
        1 -> "1" + "0".repeat(120)
        2 -> "-1" + "0".repeat(120)
        else -> buildString {
            if (random.nextBoolean()) append('-')
            append(random.nextInt(1, 10))
            repeat(random.nextInt(0, 140)) { append(random.nextInt(10)) }
        }
    }

    private fun randomEndpoint(random: Random): Int = when (random.nextInt(12)) {
        0 -> Int.MIN_VALUE
        1 -> Int.MAX_VALUE
        else -> random.nextInt(-4, 5)
    }

    private fun iteratorFailureName(action: () -> Unit): String? = try {
        action()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }

    private companion object {
        val tracePoints = listOf(Int.MIN_VALUE, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, Int.MAX_VALUE)
    }
}
