package com.bernaferrari.guavakt.parity

import com.google.common.collect.ContiguousSet as GuavaContiguousSet
import com.google.common.collect.DiscreteDomain as GuavaDiscreteDomain
import com.google.common.collect.Range as GuavaRange
import com.google.common.collect.BoundType as GuavaBoundType
import com.google.common.collect.ImmutableRangeSet as GuavaImmutableRangeSet
import com.bernaferrari.guavakt.collect.ContiguousSet as GuavaKtContiguousSet
import com.bernaferrari.guavakt.collect.DiscreteDomain as GuavaKtDiscreteDomain
import com.bernaferrari.guavakt.collect.Range as GuavaKtRange
import com.bernaferrari.guavakt.collect.BoundType as GuavaKtBoundType
import com.bernaferrari.guavakt.collect.ImmutableRangeSet as GuavaKtImmutableRangeSet
import com.bernaferrari.guavakt.math.BigInteger as GuavaKtBigInteger
import java.math.BigInteger as JavaBigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class ContiguousSetDifferentialTest {
    @Test
    fun unboundedAndOpenIntegerRangesMatchGuava() {
        val guava = listOf(
            GuavaContiguousSet.create(GuavaRange.atMost(2), GuavaDiscreteDomain.integers()),
            GuavaContiguousSet.create(GuavaRange.atLeast(-2), GuavaDiscreteDomain.integers()),
            GuavaContiguousSet.create(GuavaRange.greaterThan(Int.MAX_VALUE - 1), GuavaDiscreteDomain.integers()),
            GuavaContiguousSet.create(GuavaRange.lessThan(Int.MIN_VALUE + 1), GuavaDiscreteDomain.integers()),
        )
        val guavaKt = listOf(
            GuavaKtContiguousSet.create(GuavaKtRange.atMost(2), GuavaKtDiscreteDomain.integers()),
            GuavaKtContiguousSet.create(GuavaKtRange.atLeast(-2), GuavaKtDiscreteDomain.integers()),
            GuavaKtContiguousSet.create(GuavaKtRange.greaterThan(Int.MAX_VALUE - 1), GuavaKtDiscreteDomain.integers()),
            GuavaKtContiguousSet.create(GuavaKtRange.lessThan(Int.MIN_VALUE + 1), GuavaKtDiscreteDomain.integers()),
        )

        assertEquals(
            guava.map { listOf(it.first(), it.last(), it.size) },
            guavaKt.map { listOf(it.first(), it.last(), it.size) },
        )
    }

    @Test
    fun longDomainsAndSaturatedSizeMatchGuava() {
        val guava = GuavaContiguousSet.create(
            GuavaRange.openClosed(Long.MIN_VALUE, Long.MAX_VALUE),
            GuavaDiscreteDomain.longs(),
        )
        val guavaKt = GuavaKtContiguousSet.create(
            GuavaKtRange.openClosed(Long.MIN_VALUE, Long.MAX_VALUE),
            GuavaKtDiscreteDomain.longs(),
        )

        assertEquals(
            listOf(guava.first(), guava.last(), guava.size),
            listOf(guavaKt.first(), guavaKt.last(), guavaKt.size),
        )
    }

    @Test
    fun arbitraryPrecisionIntegerDomainMatchesGuavaWithoutArtificialBounds() {
        val start = "-100000000000000000000000000000000000000000000000000"
        val end = "100000000000000000000000000000000000000000000000000"
        val guava = GuavaContiguousSet.create(
            GuavaRange.closed(JavaBigInteger(start), JavaBigInteger(end)),
            GuavaDiscreteDomain.bigIntegers(),
        )
        val guavaKt = GuavaKtContiguousSet.create(
            GuavaKtRange.closed(GuavaKtBigInteger.parse(start), GuavaKtBigInteger.parse(end)),
            GuavaKtDiscreteDomain.bigIntegers(),
        )

        assertEquals(
            listOf(guava.first().toString(), guava.last().toString(), guava.size),
            listOf(guavaKt.first().toString(), guavaKt.last().toString(), guavaKt.size),
        )

        val guavaSmall = GuavaContiguousSet.create(
            GuavaRange.openClosed(JavaBigInteger("999999999999999999999"), JavaBigInteger("1000000000000000000002")),
            GuavaDiscreteDomain.bigIntegers(),
        )
        val guavaKtSmall = GuavaKtContiguousSet.create(
            GuavaKtRange.openClosed(GuavaKtBigInteger.parse("999999999999999999999"), GuavaKtBigInteger.parse("1000000000000000000002")),
            GuavaKtDiscreteDomain.bigIntegers(),
        )
        assertEquals(
            guavaSmall.map(Any::toString),
            guavaKtSmall.map(Any::toString),
        )
    }

    @Test
    fun factoriesEndpointsViewsAndBoundaryConversionMatchGuava() {
        val guava = GuavaContiguousSet.closed(3, 7)
        val guavaKt = GuavaKtContiguousSet.closed(3, 7)

        assertEquals(
            listOf(
                guava.first(),
                guava.last(),
                guava.toString(),
                guava.range(GuavaBoundType.OPEN, GuavaBoundType.OPEN).toString(),
                guava.headSet(5).toList(),
                guava.headSet(5, true).toList(),
                guava.tailSet(5).toList(),
                guava.subSet(4, true, 7, true).toList(),
            ),
            listOf(
                guavaKt.first(),
                guavaKt.last(),
                guavaKt.toString(),
                guavaKt.range(GuavaKtBoundType.OPEN, GuavaKtBoundType.OPEN).toString(),
                guavaKt.headSet(5).toList(),
                guavaKt.headSet(5, true).toList(),
                guavaKt.tailSet(5).toList(),
                guavaKt.subSet(4, true, 7, true).toList(),
            ),
        )

        assertEquals(
            GuavaContiguousSet.closed(Int.MIN_VALUE, Int.MAX_VALUE)
                .range(GuavaBoundType.OPEN, GuavaBoundType.OPEN)
                .toString(),
            GuavaKtContiguousSet.closed(Int.MIN_VALUE, Int.MAX_VALUE)
                .range(GuavaKtBoundType.OPEN, GuavaKtBoundType.OPEN)
                .toString(),
        )
    }

    @Test
    fun integerRangeCanonicalizationMatchesGuava() {
        val guavaDomain = GuavaDiscreteDomain.integers()
        val guavaKtDomain = GuavaKtDiscreteDomain.integers()
        val guavaRanges = listOf(
            GuavaRange.openClosed(3, 4),
            GuavaRange.open(3, 4),
            GuavaRange.atMost(Int.MIN_VALUE),
            GuavaRange.all<Int>(),
        )
        val guavaKtRanges = listOf(
            GuavaKtRange.openClosed(3, 4),
            GuavaKtRange.open(3, 4),
            GuavaKtRange.atMost(Int.MIN_VALUE),
            GuavaKtRange.all<Int>(),
        )

        assertEquals(
            guavaRanges.map { it.canonical(guavaDomain).toString() },
            guavaKtRanges.map { it.canonical(guavaKtDomain).toString() },
        )
    }

    @Test
    fun immutableRangeSetDiscreteViewMatchesGuava() {
        val guava = GuavaImmutableRangeSet.copyOf(
            listOf(GuavaRange.closed(1, 3), GuavaRange.closed(5, 6)),
        ).asSet(GuavaDiscreteDomain.integers())
        val guavaKt = GuavaKtImmutableRangeSet.copyOf(
            listOf(GuavaKtRange.closed(1, 3), GuavaKtRange.closed(5, 6)),
        ).asSet(GuavaKtDiscreteDomain.integers())

        assertEquals(
            listOf(guava.toList(), guava.size, guava.toString()),
            listOf(guavaKt.toList(), guavaKt.size, guavaKt.toString()),
        )

        val guavaLarge = GuavaImmutableRangeSet.of(GuavaRange.greaterThan(0))
            .asSet(GuavaDiscreteDomain.integers())
        val guavaKtLarge = GuavaKtImmutableRangeSet.of(GuavaKtRange.greaterThan(0))
            .asSet(GuavaKtDiscreteDomain.integers())
        assertEquals(
            listOf(guavaLarge.first(), guavaLarge.size),
            listOf(guavaKtLarge.iterator().next(), guavaKtLarge.size),
        )
    }
}
