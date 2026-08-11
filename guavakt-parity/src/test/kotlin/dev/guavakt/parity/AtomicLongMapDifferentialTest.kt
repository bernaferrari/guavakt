package dev.guavakt.parity

import com.google.common.util.concurrent.AtomicLongMap as GuavaAtomicLongMap
import dev.guavakt.util.concurrent.AtomicLongMap as GuavaKtAtomicLongMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AtomicLongMapDifferentialTest {
    @Test
    fun publicOperationTraceMatchesGuava() {
        assertEquals(guavaTrace(), guavaKtTrace())
    }

    @Test
    fun overflowAndPresentZeroEdgesMatchGuava() {
        val guava = GuavaAtomicLongMap.create(mapOf("max" to Long.MAX_VALUE))
        val guavaKt = GuavaKtAtomicLongMap.create(mapOf("max" to Long.MAX_VALUE))

        assertEquals(guava.incrementAndGet("max"), guavaKt.incrementAndGet("max"))
        assertEquals(guava.decrementAndGet("absent"), guavaKt.decrementAndGet("absent"))
        assertEquals(guava.incrementAndGet("absent"), guavaKt.incrementAndGet("absent"))
        assertEquals(guava.containsKey("absent"), guavaKt.containsKey("absent"))
        assertEquals(guava.asMap().toMap(), guavaKt.asMap())
    }

    @Test
    fun mapViewsAreCachedLiveAndRejectMutationLikeGuava() {
        val guava = GuavaAtomicLongMap.create(mapOf("a" to 1L))
        val guavaKt = GuavaKtAtomicLongMap.create(mapOf("a" to 1L))
        val guavaView = guava.asMap()
        val guavaKtView = guavaKt.asMap()

        assertSame(guavaView, guava.asMap())
        assertSame(guavaKtView, guavaKt.asMap())
        guava.put("b", 2L)
        guavaKt.put("b", 2L)
        assertEquals(guavaView.toMap(), guavaKtView)

        assertFailsWith<UnsupportedOperationException> { guavaView["c"] = 3L }
        assertFailsWith<UnsupportedOperationException> { guavaView.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { guavaView.putAll(emptyMap()) }
        @Suppress("UNCHECKED_CAST")
        val mutableGuavaKtView = guavaKtView as MutableMap<String, Long>
        assertFailsWith<UnsupportedOperationException> { mutableGuavaKtView["c"] = 3L }
        assertFailsWith<UnsupportedOperationException> { mutableGuavaKtView.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { mutableGuavaKtView.putAll(emptyMap()) }

        val guavaEntry = guavaView.entries.first()
        val guavaKtEntry = mutableGuavaKtView.entries.first()
        assertFailsWith<UnsupportedOperationException> { guavaEntry.setValue(9L) }
        assertFailsWith<UnsupportedOperationException> { guavaKtEntry.setValue(9L) }
        assertFailsWith<UnsupportedOperationException> { guavaView.keys.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { mutableGuavaKtView.keys.remove("missing") }
        assertFailsWith<UnsupportedOperationException> { guavaView.values.remove(999L) }
        assertFailsWith<UnsupportedOperationException> { mutableGuavaKtView.values.remove(999L) }
    }

    private fun guavaTrace(): List<Any> {
        val map = GuavaAtomicLongMap.create<String>()
        return buildList {
            add(map.get("x"))
            add(map.containsKey("x"))
            add(map.incrementAndGet("x"))
            add(map.getAndAdd("x", 4L))
            add(map.decrementAndGet("x"))
            add(map.updateAndGet("x") { it * 3L })
            add(map.getAndUpdate("x") { it - 2L })
            add(map.accumulateAndGet("x", 5L) { old, x -> old + x })
            add(map.getAndAccumulate("x", 2L) { old, x -> old * x })
            add(map.put("zero", 0L))
            add(map.containsKey("zero"))
            add(map.removeIfZero("zero"))
            map.putAll(mapOf("a" to 2L, "b" to -2L))
            add(map.sum())
            add(map.remove("a"))
            add(map.asMap().toSortedMap())
            add(map.size())
            add(map.isEmpty())
        }
    }

    private fun guavaKtTrace(): List<Any> {
        val map = GuavaKtAtomicLongMap.create<String>()
        return buildList {
            add(map.get("x"))
            add(map.containsKey("x"))
            add(map.incrementAndGet("x"))
            add(map.getAndAdd("x", 4L))
            add(map.decrementAndGet("x"))
            add(map.updateAndGet("x") { it * 3L })
            add(map.getAndUpdate("x") { it - 2L })
            add(map.accumulateAndGet("x", 5L) { old, x -> old + x })
            add(map.getAndAccumulate("x", 2L) { old, x -> old * x })
            add(map.put("zero", 0L))
            add(map.containsKey("zero"))
            add(map.removeIfZero("zero"))
            map.putAll(mapOf("a" to 2L, "b" to -2L))
            add(map.sum())
            add(map.remove("a"))
            add(map.asMap().toSortedMap())
            add(map.size())
            add(map.isEmpty())
        }
    }
}
