package com.bernaferrari.guavakt.parity

import com.google.common.collect.ArrayListMultimap as GuavaArrayListMultimap
import com.google.common.collect.HashMultimap as GuavaHashMultimap
import com.google.common.collect.LinkedListMultimap as GuavaLinkedListMultimap
import com.google.common.collect.Multisets as GuavaMultisets
import com.bernaferrari.guavakt.collect.ArrayListMultimap as GuavaKtArrayListMultimap
import com.bernaferrari.guavakt.collect.HashMultimap as GuavaKtHashMultimap
import com.bernaferrari.guavakt.collect.LinkedListMultimap as GuavaKtLinkedListMultimap
import com.bernaferrari.guavakt.collect.Multisets as GuavaKtMultisets
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class MultimapCoreDifferentialTest {
    @Test
    fun absentListViewAndIteratorMutationMatchGuava() {
        assertEquals(guavaListTrace(), guavaKtListTrace())
    }

    @Test
    fun setEntryViewLivenessEqualityAndRemovalMatchGuava() {
        assertEquals(guavaSetTrace(), guavaKtSetTrace())
    }

    @Test
    fun linkedListEqualityEntryMutationAndFormattingMatchGuava() {
        assertEquals(guavaLinkedListTrace(), guavaKtLinkedListTrace())
    }

    @Test
    fun keysMultisetEntryValueSemanticsMatchGuava() {
        val guava = GuavaArrayListMultimap.create<String, Int>().apply {
            put("a", 1)
            put("a", 2)
            put("b", 3)
        }
        val guavaPeer = GuavaArrayListMultimap.create(guava)
        val guavaKt = GuavaKtArrayListMultimap.create<String, Int>().apply {
            put("a", 1)
            put("a", 2)
            put("b", 3)
        }
        val guavaKtPeer = GuavaKtArrayListMultimap.create(guavaKt)

        val guavaTrace = listOf(
            guava.keys().entrySet().map { it.element to it.count }.sortedBy { it.first },
            guava.keys().entrySet().contains(GuavaMultisets.immutableEntry("a", 2)),
            guava.keys() == guavaPeer.keys(),
            guava.keys().hashCode() == guavaPeer.keys().hashCode(),
            guava.keys().toString(),
        )
        val guavaKtTrace = listOf(
            guavaKt.keys().entrySet().map { it.getElement() to it.getCount() }.sortedBy { it.first },
            guavaKt.keys().entrySet().contains(GuavaKtMultisets.immutableEntry("a", 2)),
            guavaKt.keys() == guavaKtPeer.keys(),
            guavaKt.keys().hashCode() == guavaKtPeer.keys().hashCode(),
            guavaKt.keys().toString(),
        )
        assertEquals(guavaTrace, guavaKtTrace)
    }

    @Test
    fun seededArrayListMultimapMutationTracesMatchGuava() {
        repeat(12) { seed ->
            val random = Random(seed.toLong())
            val guava = GuavaArrayListMultimap.create<String, Int>()
            val guavaKt = GuavaKtArrayListMultimap.create<String, Int>()

            repeat(240) { step ->
                val key = ('a'.code + random.nextInt(5)).toChar().toString()
                val value = random.nextInt(8)
                when (random.nextInt(11)) {
                    0 -> assertEquals(guava.put(key, value), guavaKt.put(key, value))
                    1 -> assertEquals(guava.remove(key, value), guavaKt.remove(key, value))
                    2 -> {
                        val values = List(random.nextInt(5)) { random.nextInt(8) }
                        assertEquals(guava.putAll(key, values), guavaKt.putAll(key, values))
                    }
                    3 -> assertEquals(guava.removeAll(key).toList(), guavaKt.removeAll(key).toList())
                    4 -> {
                        val values = List(random.nextInt(5)) { random.nextInt(8) }
                        assertEquals(
                            guava.replaceValues(key, values).toList(),
                            guavaKt.replaceValues(key, values).toList(),
                        )
                    }
                    5 -> {
                        val index = random.nextInt(guava.get(key).size + 1)
                        guava.get(key).add(index, value)
                        guavaKt.get(key).add(index, value)
                    }
                    6 -> if (guava.get(key).isNotEmpty()) {
                        val index = random.nextInt(guava.get(key).size)
                        assertEquals(guava.get(key).set(index, value), guavaKt.get(key).set(index, value))
                    }
                    7 -> if (guava.get(key).isNotEmpty()) {
                        val index = random.nextInt(guava.get(key).size)
                        assertEquals(guava.get(key).removeAt(index), guavaKt.get(key).removeAt(index))
                    }
                    8 -> {
                        @Suppress("UNCHECKED_CAST")
                        val guavaAsMap = guava.asMap() as MutableMap<String, Collection<Int>>
                        @Suppress("UNCHECKED_CAST")
                        val guavaKtAsMap = guavaKt.asMap() as MutableMap<String, Collection<Int>>
                        assertEquals(guavaAsMap.remove(key)?.toList(), guavaKtAsMap.remove(key)?.toList())
                    }
                    9 -> assertEquals(guava.keySet().remove(key), (guavaKt.keySet() as MutableSet<String>).remove(key))
                    10 -> assertEquals(
                        guava.entries().remove(java.util.AbstractMap.SimpleImmutableEntry(key, value)),
                        (guavaKt.entries() as MutableCollection).remove(entry(key, value)),
                    )
                }
                assertEquals(
                    guavaArrayListState(guava),
                    guavaKtArrayListState(guavaKt),
                    "seed=$seed step=$step",
                )
            }
        }
    }

    private fun guavaListTrace(): List<Any?> {
        val multimap = GuavaArrayListMultimap.create<String, Int>()
        val values = multimap.get("a")
        val beforeIterator = multimap.containsKey("a")
        val iterator = values.listIterator()
        val trace = mutableListOf<Any?>(
            beforeIterator,
            multimap.containsKey("a"),
            failureName { values[0] },
            multimap.containsKey("a"),
        )
        iterator.add(1)
        iterator.add(2)
        trace.addAll(listOf(multimap.containsKey("a"), values.toList(), multimap.size()))
        values.subList(0, 1).clear()
        trace.addAll(listOf(values.toList(), multimap.entries().map { it.key to it.value }))
        values.clear()
        trace.addAll(listOf(multimap.containsKey("a"), multimap.size()))
        return trace
    }

    private fun guavaKtListTrace(): List<Any?> {
        val multimap = GuavaKtArrayListMultimap.create<String, Int>()
        val values = multimap.get("a")
        val beforeIterator = multimap.containsKey("a")
        val iterator = values.listIterator()
        val trace = mutableListOf<Any?>(
            beforeIterator,
            multimap.containsKey("a"),
            failureName { values[0] },
            multimap.containsKey("a"),
        )
        iterator.add(1)
        iterator.add(2)
        trace.addAll(listOf(multimap.containsKey("a"), values.toList(), multimap.size()))
        values.subList(0, 1).clear()
        trace.addAll(listOf(values.toList(), multimap.entries().map { it.key to it.value }))
        values.clear()
        trace.addAll(listOf(multimap.containsKey("a"), multimap.size()))
        return trace
    }

    private fun guavaSetTrace(): List<Any?> {
        val multimap = GuavaHashMultimap.create<String, Int>()
        multimap.put("a", 1)
        val entries = multimap.entries()
        val trace = mutableListOf<Any?>(entryPairs(entries))
        multimap.put("b", 2)
        trace.addAll(listOf(
            entryPairs(entries),
            entries.contains(java.util.AbstractMap.SimpleImmutableEntry("a", 1)),
            entries == setOf(
                java.util.AbstractMap.SimpleImmutableEntry("a", 1),
                java.util.AbstractMap.SimpleImmutableEntry("b", 2),
            ),
            entries.remove(java.util.AbstractMap.SimpleImmutableEntry("a", 1)),
            entryPairs(multimap.entries()),
        ))
        val iterator = entries.iterator()
        trace.add(iterator.next().let { it.key to it.value })
        iterator.remove()
        trace.addAll(listOf(multimap.isEmpty(), entryPairs(entries)))
        multimap.put("c", 3)
        trace.add(entryPairs(entries))
        return trace
    }

    private fun guavaKtSetTrace(): List<Any?> {
        val multimap = GuavaKtHashMultimap.create<String, Int>()
        multimap.put("a", 1)
        val entries = multimap.entries()
        val trace = mutableListOf<Any?>(entryPairs(entries))
        multimap.put("b", 2)
        trace.addAll(listOf(
            entryPairs(entries),
            entries.contains(entry("a", 1)),
            entries == setOf(entry("a", 1), entry("b", 2)),
            (entries as MutableSet).remove(entry("a", 1)),
            entryPairs(multimap.entries()),
        ))
        val iterator = entries.iterator() as MutableIterator<Map.Entry<String, Int>>
        trace.add(iterator.next().let { it.key to it.value })
        iterator.remove()
        trace.addAll(listOf(multimap.isEmpty(), entryPairs(entries)))
        multimap.put("c", 3)
        trace.add(entryPairs(entries))
        return trace
    }

    private fun guavaLinkedListTrace(): List<Any?> {
        val first = GuavaLinkedListMultimap.create<String, Int>().apply {
            put("a", 1)
            put("b", 2)
            put("a", 3)
        }
        val second = GuavaLinkedListMultimap.create(first)
        val entry = first.entries().first()
        return listOf(
            first == second,
            first.hashCode() == second.hashCode(),
            first.toString(),
            first.entries().contains(java.util.AbstractMap.SimpleImmutableEntry("a", 1)),
            entry.setValue(10),
            first.entries().map { it.key to it.value },
        )
    }

    private fun guavaKtLinkedListTrace(): List<Any?> {
        val first = GuavaKtLinkedListMultimap.create<String, Int>().apply {
            put("a", 1)
            put("b", 2)
            put("a", 3)
        }
        val second = GuavaKtLinkedListMultimap.create(first)
        val entry = first.entries().first() as MutableMap.MutableEntry<String, Int>
        return listOf(
            first == second,
            first.hashCode() == second.hashCode(),
            first.toString(),
            first.entries().contains(entry("a", 1)),
            entry.setValue(10),
            first.entries().map { it.key to it.value },
        )
    }

    private fun guavaArrayListState(multimap: GuavaArrayListMultimap<String, Int>): List<Any> =
        listOf(
            multimap.size(),
            multimap.keySet().sorted().map { key -> key to multimap.get(key).toList() },
            multimap.keys().entrySet().map { it.element to it.count }.sortedBy { it.first },
            multimap.containsValue(0),
            multimap.containsEntry("a", 0),
        )

    private fun guavaKtArrayListState(multimap: GuavaKtArrayListMultimap<String, Int>): List<Any> =
        listOf(
            multimap.size(),
            multimap.keySet().sorted().map { key -> key to multimap.get(key).toList() },
            multimap.keys().entrySet().map { it.getElement() to it.getCount() }.sortedBy { it.first },
            multimap.containsValue(0),
            multimap.containsEntry("a", 0),
        )

    private fun entry(key: String, value: Int): Map.Entry<String, Int> =
        object : Map.Entry<String, Int> {
            override val key: String = key
            override val value: Int = value
            override fun equals(other: Any?): Boolean =
                other is Map.Entry<*, *> && key == other.key && value == other.value
            override fun hashCode(): Int = key.hashCode() xor value.hashCode()
            override fun toString(): String = "$key=$value"
        }

    private fun entryPairs(entries: Collection<Map.Entry<String, Int>>): List<Pair<String, Int>> =
        entries.map { it.key to it.value }.sortedWith(compareBy({ it.first }, { it.second }))

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
