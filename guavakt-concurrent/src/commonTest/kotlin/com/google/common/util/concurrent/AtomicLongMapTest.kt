package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AtomicLongMapTest {
    @Test
    fun absentAndExplicitZeroAreDistinguishedOnlyByMapOperations() {
        val map = AtomicLongMap.create<String>()

        assertEquals(0L, map.get("key"))
        assertFalse(map.containsKey("key"))
        assertEquals(0L, map.put("key", 0L))
        assertEquals(0L, map.get("key"))
        assertTrue(map.containsKey("key"))
        assertEquals(1, map.size())
        assertEquals(mapOf("key" to 0L), map.asMap())

        assertTrue(map.removeIfZero("key"))
        assertFalse(map.containsKey("key"))
        assertFalse(map.removeIfZero("key"))
    }

    @Test
    fun arithmeticOperationsReturnTheCorrectSideOfTheUpdate() {
        val map = AtomicLongMap.create<String>()

        assertEquals(1L, map.incrementAndGet("n"))
        assertEquals(1L, map.getAndIncrement("n"))
        assertEquals(2L, map.getAndAdd("n", 5L))
        assertEquals(6L, map.decrementAndGet("n"))
        assertEquals(6L, map.getAndDecrement("n"))
        assertEquals(12L, map.addAndGet("n", 7L))
        assertEquals(12L, map.get("n"))
    }

    @Test
    fun longOverflowMatchesGuavaAndJvmTwoComplementBehavior() {
        val map = AtomicLongMap.create(mapOf("max" to Long.MAX_VALUE, "min" to Long.MIN_VALUE))

        assertEquals(Long.MIN_VALUE, map.incrementAndGet("max"))
        assertEquals(Long.MAX_VALUE, map.decrementAndGet("min"))
    }

    @Test
    fun updateAndAccumulateCallbacksRunExactlyOnce() {
        val map = AtomicLongMap.create<String>()
        var calls = 0

        assertEquals(7L, map.updateAndGet("x") { old -> calls++; old + 7L })
        assertEquals(7L, map.getAndUpdate("x") { old -> calls++; old * 2L })
        assertEquals(17L, map.accumulateAndGet("x", 3L) { old, x -> calls++; old + x })
        assertEquals(17L, map.getAndAccumulate("x", 2L) { old, x -> calls++; old * x })
        assertEquals(34L, map.get("x"))
        assertEquals(4, calls)
    }

    @Test
    fun throwingUpdaterLeavesTheMappingUnchangedAndReleasesWriterGate() {
        val map = AtomicLongMap.create(mapOf("x" to 4L))

        assertFailsWith<IllegalStateException> {
            map.updateAndGet("x") { throw IllegalStateException("boom") }
        }
        assertEquals(4L, map.get("x"))
        assertEquals(5L, map.incrementAndGet("x"))
    }

    @Test
    fun conditionalPackageOperationsMatchGuavaZeroRules() {
        val map = AtomicLongMap.create<String>()

        assertFalse(map.remove("x", 0L))
        assertEquals(0L, map.putIfAbsent("x", 3L))
        assertEquals(3L, map.putIfAbsent("x", 9L))
        assertFalse(map.replace("x", 0L, 4L))
        assertTrue(map.replace("x", 3L, 0L))
        assertEquals(0L, map.putIfAbsent("x", 8L))
        assertTrue(map.remove("x", 8L))

        assertTrue(map.replace("absent", 0L, 6L))
        assertEquals(6L, map.get("absent"))
    }

    @Test
    fun removeAllZerosSumPutAllAndClearPreserveExpectedMappings() {
        val map = AtomicLongMap.create(mapOf("a" to 0L, "b" to 2L))
        map.putAll(mapOf("c" to -2L, "d" to 5L))

        assertEquals(5L, map.sum())
        map.removeAllZeros()
        assertEquals(mapOf("b" to 2L, "c" to -2L, "d" to 5L), map.asMap())
        assertEquals(-2L, map.remove("c"))
        assertEquals(0L, map.remove("missing"))
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun createCopiesItsInput() {
        val input = mutableMapOf("a" to 1L)
        val map = AtomicLongMap.create(input)

        input["a"] = 99L
        input["b"] = 2L
        assertEquals(mapOf("a" to 1L), map.asMap())
    }

    @Test
    fun asMapIsCachedLiveAndRejectsEveryMutationRoute() {
        val map = AtomicLongMap.create(mapOf("a" to 1L))
        val view = map.asMap()
        val entries = view.entries

        assertSame(view, map.asMap())
        map.put("b", 2L)
        assertEquals(2, view.size)
        assertEquals(2, entries.size)
        assertEquals(2L, view["b"])

        @Suppress("UNCHECKED_CAST")
        val mutableView = view as MutableMap<String, Long>
        assertFailsWith<UnsupportedOperationException> { mutableView["c"] = 3L }
        assertFailsWith<UnsupportedOperationException> { mutableView.remove("a") }
        assertFailsWith<UnsupportedOperationException> { mutableView.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { mutableView.putAll(emptyMap()) }
        assertFailsWith<UnsupportedOperationException> { mutableView.clear() }

        @Suppress("UNCHECKED_CAST")
        val mutableEntries = entries as MutableSet<MutableMap.MutableEntry<String, Long>>
        val entry = mutableEntries.first()
        assertFailsWith<UnsupportedOperationException> { entry.setValue(9L) }
        assertFailsWith<UnsupportedOperationException> { mutableEntries.iterator().remove() }
        assertFailsWith<UnsupportedOperationException> { mutableEntries.remove(entry) }
        assertFailsWith<UnsupportedOperationException> { mutableEntries.removeAll(emptySet()) }
        assertFailsWith<UnsupportedOperationException> { mutableEntries.retainAll(mutableEntries.toSet()) }
        assertFailsWith<UnsupportedOperationException> { mutableEntries.clear() }

        val mutableKeys = view.keys
        assertFailsWith<UnsupportedOperationException> { mutableKeys.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { mutableKeys.clear() }

        val mutableValues = view.values
        assertFailsWith<UnsupportedOperationException> { mutableValues.remove(999L) }
        assertFailsWith<UnsupportedOperationException> { mutableValues.clear() }
        assertEquals(mapOf("a" to 1L, "b" to 2L), view)
    }

    @Test
    fun collidingAndExtremeHashCodesRemainDistinct() {
        val collisionA = HashKey("a", 7)
        val collisionB = HashKey("b", 7)
        val minimumHash = HashKey("min", Int.MIN_VALUE)
        val map = AtomicLongMap.create<HashKey>()

        map.put(collisionA, 1L)
        map.put(collisionB, 2L)
        map.put(minimumHash, 3L)
        assertEquals(2L, map.incrementAndGet(collisionA))

        assertEquals(2L, map.get(collisionA))
        assertEquals(2L, map.get(collisionB))
        assertEquals(3L, map.get(minimumHash))
        assertEquals(3, map.size())
        assertEquals(setOf(collisionA, collisionB, minimumHash), map.asMap().keys)
    }

    @Test
    fun existingIteratorUsesAStableSnapshotWhileEntrySetRemainsLive() {
        val map = AtomicLongMap.create(mapOf("a" to 1L, "b" to 2L))
        val entries = map.asMap().entries
        val iterator = entries.iterator()

        map.put("c", 3L)
        val iteratedKeys = buildSet {
            while (iterator.hasNext()) add(iterator.next().key)
        }

        assertEquals(setOf("a", "b"), iteratedKeys)
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.key == "c" && it.value == 3L })
    }

    private class HashKey(private val name: String, private val hash: Int) {
        override fun equals(other: Any?): Boolean = other is HashKey && name == other.name
        override fun hashCode(): Int = hash
        override fun toString(): String = name
    }
}
