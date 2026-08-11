package dev.guavakt.parity

import com.google.common.collect.ArrayListMultimap as GuavaArrayListMultimap
import com.google.common.collect.Multimaps as GuavaMultimaps
import dev.guavakt.collect.ArrayListMultimap
import dev.guavakt.collect.Multimaps
import kotlin.test.Test
import kotlin.test.assertEquals

class UnmodifiableMultimapDifferentialTest {
    @Test
    fun heldViewsStayLiveAndRejectMutation() {
        val guava = GuavaArrayListMultimap.create<String, Int>()
        val ours = ArrayListMultimap.create<String, Int>()
        val expected = trace(guava, GuavaMultimaps.unmodifiableMultimap(guava))
        val actual = trace(ours, Multimaps.unmodifiableMultimap(ours))
        assertEquals(expected, actual)
    }

    private fun trace(
        source: GuavaArrayListMultimap<String, Int>,
        view: com.google.common.collect.Multimap<String, Int>,
    ): List<Any?> {
        val values = view.get("a")
        val keys = view.keySet()
        val allValues = view.values()
        val entries = view.entries()
        val asMap = view.asMap()
        source.put("a", 1)
        source.put("b", 2)
        val mapEntry = asMap.entries.first { it.key == "a" }
        return listOf(values.toList(), keys.toList(), allValues.toList(), entries.map { it.key to it.value }, asMap["a"]?.toList(), asMap.keys.toList(), mapEntry == java.util.AbstractMap.SimpleImmutableEntry("a", listOf(1)), mapEntry.hashCode(), mapEntry.toString(), outcome { values.add(3) }, outcome { (asMap["a"] as MutableCollection<Int>).add(3) }, outcome { (asMap as MutableMap<String, Collection<Int>>).remove("a") })
    }

    private fun trace(source: ArrayListMultimap<String, Int>, view: dev.guavakt.collect.Multimap<String, Int>): List<Any?> {
        val values = view.get("a")
        val keys = view.keySet()
        val allValues = view.values()
        val entries = view.entries()
        val asMap = view.asMap()
        source.put("a", 1)
        source.put("b", 2)
        val mapEntry = asMap.entries.first { it.key == "a" }
        return listOf(values.toList(), keys.toList(), allValues.toList(), entries.map { it.key to it.value }, asMap["a"]?.toList(), asMap.keys.toList(), mapEntry == java.util.AbstractMap.SimpleImmutableEntry("a", listOf(1)), mapEntry.hashCode(), mapEntry.toString(), outcome { values.add(3) }, outcome { (asMap["a"] as MutableCollection<Int>).add(3) }, outcome { (asMap as MutableMap<String, Collection<Int>>).remove("a") })
    }

    private fun <T> outcome(action: () -> T): Any? = try { action() } catch (failure: Throwable) { failure::class.simpleName }
}
