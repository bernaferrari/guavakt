package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BloomFilterTest {
    @Test
    fun putMembershipCopyAndPredicateAliases() {
        val filter = BloomFilter.create(Funnels.unencodedCharsFunnel(), 100, 0.01)
        assertTrue(filter.put("apple"))
        assertFalse(filter.put("apple"))
        assertTrue(filter.put("banana"))
        assertTrue(filter.mightContain("apple"))
        assertTrue(filter.apply("banana"))
        assertTrue(filter.test("apple"))
        assertTrue(filter("banana"))
        assertFalse(filter.mightContain("this-string-was-never-inserted-zzqx"))

        val copy = filter.copy()
        assertEquals(filter, copy)
        assertEquals(filter.hashCode(), copy.hashCode())
        copy.put("cherry")
        assertNotEquals(filter, copy)
        assertFalse(filter.mightContain("cherry"))
    }

    @Test
    fun compatibilityAndUnionRespectShapeFunnelAndIdentity() {
        val first = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01)
        val second = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01)
        first.put(1)
        second.put(2)
        assertTrue(first.isCompatible(second))
        assertFalse(first.isCompatible(first))
        first.putAll(second)
        assertTrue(first.mightContain(1))
        assertTrue(first.mightContain(2))
        assertFailsWith<IllegalArgumentException> { first.putAll(first) }

        val differentSize = BloomFilter.create(Funnels.integerFunnel(), 1_000, 0.01)
        assertFalse(first.isCompatible(differentSize))
        assertFailsWith<IllegalArgumentException> { first.putAll(differentSize) }

        val custom = Funnel<Int> { value, sink -> sink.putInt(value) }
        val differentFunnel = BloomFilter.create(custom, 100, 0.01)
        assertFalse(first.isCompatible(differentFunnel))
        assertFailsWith<IllegalArgumentException> { first.putAll(differentFunnel) }
    }

    @Test
    fun wireRoundTripPreservesBitsCountsAndCompatibility() {
        val original = BloomFilter.create(Funnels.integerFunnel(), 100, 0.01)
        (0 until 35).forEach(original::put)
        val bytes = original.toByteArray()
        val restored = BloomFilter.readFrom(bytes, Funnels.integerFunnel())
        assertEquals(original, restored)
        assertTrue(original.isCompatible(restored))
        assertEquals(original.expectedFpp(), restored.expectedFpp())
        assertEquals(original.approximateElementCount(), restored.approximateElementCount())
        assertEquals(bytes.size.toLong(), original.serializedSize())
        (0 until 35).forEach { assertTrue(restored.mightContain(it)) }
    }

    @Test
    fun malformedWireAndFactoryArgumentsAreRejected() {
        assertFailsWith<IllegalArgumentException> { BloomFilter.create(Funnels.integerFunnel(), -1) }
        assertFailsWith<IllegalArgumentException> { BloomFilter.create(Funnels.integerFunnel(), 1, 0.0) }
        assertFailsWith<IllegalArgumentException> { BloomFilter.create(Funnels.integerFunnel(), 1, 1.0) }
        assertFailsWith<IllegalArgumentException> { BloomFilter.create(Funnels.integerFunnel(), 1, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { BloomFilter.readFrom(byteArrayOf(1), Funnels.integerFunnel()) }

        val valid = BloomFilter.create(Funnels.integerFunnel(), 10).toByteArray()
        assertFailsWith<IllegalArgumentException> {
            BloomFilter.readFrom(valid.copyOf().also { it[0] = 9 }, Funnels.integerFunnel())
        }
        assertFailsWith<IllegalArgumentException> {
            BloomFilter.readFrom(valid.copyOf().also { it[1] = 0 }, Funnels.integerFunnel())
        }
        assertFailsWith<IllegalArgumentException> {
            BloomFilter.readFrom(valid.copyOf(valid.size - 1), Funnels.integerFunnel())
        }
    }

    @Test
    fun sizingFppAndCardinalityAreDeterministic() {
        assertEquals(14L, BloomFilter.create(Funnels.integerFunnel(), 0).serializedSize())
        assertEquals(14L, BloomFilter.create(Funnels.integerFunnel(), 1).serializedSize())
        assertEquals(126L, BloomFilter.create(Funnels.integerFunnel(), 100, 0.01).serializedSize())
        assertEquals(1_206L, BloomFilter.create(Funnels.integerFunnel(), 1_000, 0.01).serializedSize())

        val filter = BloomFilter.create(Funnels.longFunnel(), 500, 0.01)
        assertEquals(0.0, filter.expectedFpp())
        assertEquals(0L, filter.approximateElementCount())
        (0L until 120L).forEach(filter::put)
        assertTrue(filter.expectedFpp() in 0.0..0.01)
        assertTrue(filter.approximateElementCount() in 115L..125L)
    }

    @Test
    fun standardAndSequentialFunnelsHaveStableCompatibilityEquality() {
        assertTrue(Funnels.integerFunnel() === Funnels.integerFunnel())
        assertEquals(Funnels.unencodedCharsFunnel(), Funnels.stringFunnel())
        assertEquals(
            Funnels.sequentialFunnel(Funnels.integerFunnel()),
            Funnels.sequentialFunnel(Funnels.integerFunnel()),
        )
    }
}
