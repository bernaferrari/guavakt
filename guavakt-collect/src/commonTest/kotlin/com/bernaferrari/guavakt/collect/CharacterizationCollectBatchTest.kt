package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CharacterizationCollectBatchTest {
  @Test fun immutableList_empty() { assertTrue(ImmutableList.of<Int>().isEmpty()) }
  @Test fun immutableList_of_1() { assertEquals(1, ImmutableList.of(0).size) }
  @Test fun immutableList_of_2() { assertEquals(2, ImmutableList.of(0, 1).size) }
  @Test fun immutableList_of_3() { assertEquals(3, ImmutableList.of(0, 1, 2).size) }
  @Test fun immutableList_of_4() { assertEquals(4, ImmutableList.of(0, 1, 2, 3).size) }
  @Test fun immutableList_of_5() { assertEquals(5, ImmutableList.of(0, 1, 2, 3, 4).size) }
  @Test fun immutableList_of_6() { assertEquals(6, ImmutableList.of(0, 1, 2, 3, 4, 5).size) }
  @Test fun immutableList_of_7() { assertEquals(7, ImmutableList.of(0, 1, 2, 3, 4, 5, 6).size) }
  @Test fun immutableList_of_8() { assertEquals(8, ImmutableList.of(0, 1, 2, 3, 4, 5, 6, 7).size) }
  @Test fun immutableList_of_9() { assertEquals(9, ImmutableList.of(0, 1, 2, 3, 4, 5, 6, 7, 8).size) }
  @Test fun immutableList_of_10() { assertEquals(10, ImmutableList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).size) }
  @Test fun immutableList_of_11() { assertEquals(11, ImmutableList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).size) }
  @Test fun hashMultimap_put_0() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 0))
    assertTrue(m.containsEntry("k", 0))
  }
  @Test fun hashMultimap_put_1() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 1))
    assertTrue(m.containsEntry("k", 1))
  }
  @Test fun hashMultimap_put_2() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 2))
    assertTrue(m.containsEntry("k", 2))
  }
  @Test fun hashMultimap_put_3() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 3))
    assertTrue(m.containsEntry("k", 3))
  }
  @Test fun hashMultimap_put_4() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 4))
    assertTrue(m.containsEntry("k", 4))
  }
  @Test fun hashMultimap_put_5() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 5))
    assertTrue(m.containsEntry("k", 5))
  }
  @Test fun hashMultimap_put_6() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 6))
    assertTrue(m.containsEntry("k", 6))
  }
  @Test fun hashMultimap_put_7() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 7))
    assertTrue(m.containsEntry("k", 7))
  }
  @Test fun hashMultimap_put_8() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 8))
    assertTrue(m.containsEntry("k", 8))
  }
  @Test fun hashMultimap_put_9() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 9))
    assertTrue(m.containsEntry("k", 9))
  }
  @Test fun hashMultimap_put_10() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 10))
    assertTrue(m.containsEntry("k", 10))
  }
  @Test fun hashMultimap_put_11() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 11))
    assertTrue(m.containsEntry("k", 11))
  }
  @Test fun hashMultimap_put_12() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 12))
    assertTrue(m.containsEntry("k", 12))
  }
  @Test fun hashMultimap_put_13() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 13))
    assertTrue(m.containsEntry("k", 13))
  }
  @Test fun hashMultimap_put_14() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 14))
    assertTrue(m.containsEntry("k", 14))
  }
  @Test fun hashMultimap_put_15() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 15))
    assertTrue(m.containsEntry("k", 15))
  }
  @Test fun hashMultimap_put_16() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 16))
    assertTrue(m.containsEntry("k", 16))
  }
  @Test fun hashMultimap_put_17() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 17))
    assertTrue(m.containsEntry("k", 17))
  }
  @Test fun hashMultimap_put_18() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 18))
    assertTrue(m.containsEntry("k", 18))
  }
  @Test fun hashMultimap_put_19() {
    val m = HashMultimap.create<String, Int>()
    assertTrue(m.put("k", 19))
    assertTrue(m.containsEntry("k", 19))
  }
  @Test fun arrayListMultimap_get_0() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(0, "v")
    assertEquals(listOf("v"), m.get(0))
  }
  @Test fun arrayListMultimap_get_1() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(1, "v")
    assertEquals(listOf("v"), m.get(1))
  }
  @Test fun arrayListMultimap_get_2() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(2, "v")
    assertEquals(listOf("v"), m.get(2))
  }
  @Test fun arrayListMultimap_get_3() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(3, "v")
    assertEquals(listOf("v"), m.get(3))
  }
  @Test fun arrayListMultimap_get_4() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(4, "v")
    assertEquals(listOf("v"), m.get(4))
  }
  @Test fun arrayListMultimap_get_5() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(5, "v")
    assertEquals(listOf("v"), m.get(5))
  }
  @Test fun arrayListMultimap_get_6() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(6, "v")
    assertEquals(listOf("v"), m.get(6))
  }
  @Test fun arrayListMultimap_get_7() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(7, "v")
    assertEquals(listOf("v"), m.get(7))
  }
  @Test fun arrayListMultimap_get_8() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(8, "v")
    assertEquals(listOf("v"), m.get(8))
  }
  @Test fun arrayListMultimap_get_9() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(9, "v")
    assertEquals(listOf("v"), m.get(9))
  }
  @Test fun arrayListMultimap_get_10() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(10, "v")
    assertEquals(listOf("v"), m.get(10))
  }
  @Test fun arrayListMultimap_get_11() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(11, "v")
    assertEquals(listOf("v"), m.get(11))
  }
  @Test fun arrayListMultimap_get_12() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(12, "v")
    assertEquals(listOf("v"), m.get(12))
  }
  @Test fun arrayListMultimap_get_13() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(13, "v")
    assertEquals(listOf("v"), m.get(13))
  }
  @Test fun arrayListMultimap_get_14() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(14, "v")
    assertEquals(listOf("v"), m.get(14))
  }
  @Test fun arrayListMultimap_get_15() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(15, "v")
    assertEquals(listOf("v"), m.get(15))
  }
  @Test fun arrayListMultimap_get_16() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(16, "v")
    assertEquals(listOf("v"), m.get(16))
  }
  @Test fun arrayListMultimap_get_17() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(17, "v")
    assertEquals(listOf("v"), m.get(17))
  }
  @Test fun arrayListMultimap_get_18() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(18, "v")
    assertEquals(listOf("v"), m.get(18))
  }
  @Test fun arrayListMultimap_get_19() {
    val m = ArrayListMultimap.create<Int, String>()
    m.put(19, "v")
    assertEquals(listOf("v"), m.get(19))
  }
  @Test fun hashMultiset_count_0() {
    val ms = HashMultiset.create<Int>()
    repeat(0) { ms.add(7) }
    assertEquals(0, ms.count(7))
  }
  @Test fun hashMultiset_count_1() {
    val ms = HashMultiset.create<Int>()
    repeat(1) { ms.add(7) }
    assertEquals(1, ms.count(7))
  }
  @Test fun hashMultiset_count_2() {
    val ms = HashMultiset.create<Int>()
    repeat(2) { ms.add(7) }
    assertEquals(2, ms.count(7))
  }
  @Test fun hashMultiset_count_3() {
    val ms = HashMultiset.create<Int>()
    repeat(3) { ms.add(7) }
    assertEquals(3, ms.count(7))
  }
  @Test fun hashMultiset_count_4() {
    val ms = HashMultiset.create<Int>()
    repeat(4) { ms.add(7) }
    assertEquals(4, ms.count(7))
  }
  @Test fun hashMultiset_count_5() {
    val ms = HashMultiset.create<Int>()
    repeat(5) { ms.add(7) }
    assertEquals(5, ms.count(7))
  }
  @Test fun hashMultiset_count_6() {
    val ms = HashMultiset.create<Int>()
    repeat(6) { ms.add(7) }
    assertEquals(6, ms.count(7))
  }
  @Test fun hashMultiset_count_7() {
    val ms = HashMultiset.create<Int>()
    repeat(7) { ms.add(7) }
    assertEquals(7, ms.count(7))
  }
  @Test fun hashMultiset_count_8() {
    val ms = HashMultiset.create<Int>()
    repeat(8) { ms.add(7) }
    assertEquals(8, ms.count(7))
  }
  @Test fun hashMultiset_count_9() {
    val ms = HashMultiset.create<Int>()
    repeat(9) { ms.add(7) }
    assertEquals(9, ms.count(7))
  }
  @Test fun hashMultiset_count_10() {
    val ms = HashMultiset.create<Int>()
    repeat(10) { ms.add(7) }
    assertEquals(10, ms.count(7))
  }
  @Test fun hashMultiset_count_11() {
    val ms = HashMultiset.create<Int>()
    repeat(11) { ms.add(7) }
    assertEquals(11, ms.count(7))
  }
  @Test fun hashMultiset_count_12() {
    val ms = HashMultiset.create<Int>()
    repeat(12) { ms.add(7) }
    assertEquals(12, ms.count(7))
  }
  @Test fun hashMultiset_count_13() {
    val ms = HashMultiset.create<Int>()
    repeat(13) { ms.add(7) }
    assertEquals(13, ms.count(7))
  }
  @Test fun hashMultiset_count_14() {
    val ms = HashMultiset.create<Int>()
    repeat(14) { ms.add(7) }
    assertEquals(14, ms.count(7))
  }
  @Test fun maps_newHashMap_0() {
    val m = Maps.newHashMap<Int, Int>()
    m[0] = 0
    assertEquals(0, m[0])
  }
  @Test fun maps_newHashMap_1() {
    val m = Maps.newHashMap<Int, Int>()
    m[1] = 2
    assertEquals(2, m[1])
  }
  @Test fun maps_newHashMap_2() {
    val m = Maps.newHashMap<Int, Int>()
    m[2] = 4
    assertEquals(4, m[2])
  }
  @Test fun maps_newHashMap_3() {
    val m = Maps.newHashMap<Int, Int>()
    m[3] = 6
    assertEquals(6, m[3])
  }
  @Test fun maps_newHashMap_4() {
    val m = Maps.newHashMap<Int, Int>()
    m[4] = 8
    assertEquals(8, m[4])
  }
  @Test fun maps_newHashMap_5() {
    val m = Maps.newHashMap<Int, Int>()
    m[5] = 10
    assertEquals(10, m[5])
  }
  @Test fun maps_newHashMap_6() {
    val m = Maps.newHashMap<Int, Int>()
    m[6] = 12
    assertEquals(12, m[6])
  }
  @Test fun maps_newHashMap_7() {
    val m = Maps.newHashMap<Int, Int>()
    m[7] = 14
    assertEquals(14, m[7])
  }
  @Test fun maps_newHashMap_8() {
    val m = Maps.newHashMap<Int, Int>()
    m[8] = 16
    assertEquals(16, m[8])
  }
  @Test fun maps_newHashMap_9() {
    val m = Maps.newHashMap<Int, Int>()
    m[9] = 18
    assertEquals(18, m[9])
  }
  @Test fun maps_newHashMap_10() {
    val m = Maps.newHashMap<Int, Int>()
    m[10] = 20
    assertEquals(20, m[10])
  }
  @Test fun maps_newHashMap_11() {
    val m = Maps.newHashMap<Int, Int>()
    m[11] = 22
    assertEquals(22, m[11])
  }
  @Test fun maps_newHashMap_12() {
    val m = Maps.newHashMap<Int, Int>()
    m[12] = 24
    assertEquals(24, m[12])
  }
  @Test fun maps_newHashMap_13() {
    val m = Maps.newHashMap<Int, Int>()
    m[13] = 26
    assertEquals(26, m[13])
  }
  @Test fun maps_newHashMap_14() {
    val m = Maps.newHashMap<Int, Int>()
    m[14] = 28
    assertEquals(28, m[14])
  }
  @Test fun sets_newHashSet_0() {
    val s = Sets.newHashSet(0, 0+1)
    assertTrue(s.contains(0))
  }
  @Test fun sets_newHashSet_1() {
    val s = Sets.newHashSet(1, 1+1)
    assertTrue(s.contains(1))
  }
  @Test fun sets_newHashSet_2() {
    val s = Sets.newHashSet(2, 2+1)
    assertTrue(s.contains(2))
  }
  @Test fun sets_newHashSet_3() {
    val s = Sets.newHashSet(3, 3+1)
    assertTrue(s.contains(3))
  }
  @Test fun sets_newHashSet_4() {
    val s = Sets.newHashSet(4, 4+1)
    assertTrue(s.contains(4))
  }
  @Test fun sets_newHashSet_5() {
    val s = Sets.newHashSet(5, 5+1)
    assertTrue(s.contains(5))
  }
  @Test fun sets_newHashSet_6() {
    val s = Sets.newHashSet(6, 6+1)
    assertTrue(s.contains(6))
  }
  @Test fun sets_newHashSet_7() {
    val s = Sets.newHashSet(7, 7+1)
    assertTrue(s.contains(7))
  }
  @Test fun sets_newHashSet_8() {
    val s = Sets.newHashSet(8, 8+1)
    assertTrue(s.contains(8))
  }
  @Test fun sets_newHashSet_9() {
    val s = Sets.newHashSet(9, 9+1)
    assertTrue(s.contains(9))
  }
  @Test fun sets_newHashSet_10() {
    val s = Sets.newHashSet(10, 10+1)
    assertTrue(s.contains(10))
  }
  @Test fun sets_newHashSet_11() {
    val s = Sets.newHashSet(11, 11+1)
    assertTrue(s.contains(11))
  }
  @Test fun sets_newHashSet_12() {
    val s = Sets.newHashSet(12, 12+1)
    assertTrue(s.contains(12))
  }
  @Test fun sets_newHashSet_13() {
    val s = Sets.newHashSet(13, 13+1)
    assertTrue(s.contains(13))
  }
  @Test fun sets_newHashSet_14() {
    val s = Sets.newHashSet(14, 14+1)
    assertTrue(s.contains(14))
  }
  @Test fun ints_compare_1_2() {
    val c = com.bernaferrari.guavakt.primitives.Ints.compare(1, 2)
    assertEquals((1).compareTo(2), c)
  }
  @Test fun ints_compare_2_1() {
    val c = com.bernaferrari.guavakt.primitives.Ints.compare(2, 1)
    assertEquals((2).compareTo(1), c)
  }
  @Test fun ints_compare_0_0() {
    val c = com.bernaferrari.guavakt.primitives.Ints.compare(0, 0)
    assertEquals((0).compareTo(0), c)
  }
  @Test fun ints_compare_5_5() {
    val c = com.bernaferrari.guavakt.primitives.Ints.compare(5, 5)
    assertEquals((5).compareTo(5), c)
  }
  @Test fun ints_compare_3_4() {
    val c = com.bernaferrari.guavakt.primitives.Ints.compare(3, 4)
    assertEquals((3).compareTo(4), c)
  }
  @Test fun range_closed_0() {
    val r = Range.closed(0, 0+5)
    assertTrue(r.contains(0+2))
  }
  @Test fun range_closed_1() {
    val r = Range.closed(1, 1+5)
    assertTrue(r.contains(1+2))
  }
  @Test fun range_closed_2() {
    val r = Range.closed(2, 2+5)
    assertTrue(r.contains(2+2))
  }
  @Test fun range_closed_3() {
    val r = Range.closed(3, 3+5)
    assertTrue(r.contains(3+2))
  }
  @Test fun range_closed_4() {
    val r = Range.closed(4, 4+5)
    assertTrue(r.contains(4+2))
  }
  @Test fun range_closed_5() {
    val r = Range.closed(5, 5+5)
    assertTrue(r.contains(5+2))
  }
  @Test fun range_closed_6() {
    val r = Range.closed(6, 6+5)
    assertTrue(r.contains(6+2))
  }
  @Test fun range_closed_7() {
    val r = Range.closed(7, 7+5)
    assertTrue(r.contains(7+2))
  }
  @Test fun range_closed_8() {
    val r = Range.closed(8, 8+5)
    assertTrue(r.contains(8+2))
  }
  @Test fun range_closed_9() {
    val r = Range.closed(9, 9+5)
    assertTrue(r.contains(9+2))
  }
  @Test fun compactHashMap_0() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(0, 0)
    assertEquals(0, m[0])
    m.trimToSize()
    assertEquals(0, m[0])
  }
  @Test fun compactHashMap_1() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(1, 1)
    assertEquals(1, m[1])
    m.trimToSize()
    assertEquals(1, m[1])
  }
  @Test fun compactHashMap_2() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(2, 2)
    assertEquals(2, m[2])
    m.trimToSize()
    assertEquals(2, m[2])
  }
  @Test fun compactHashMap_3() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(3, 3)
    assertEquals(3, m[3])
    m.trimToSize()
    assertEquals(3, m[3])
  }
  @Test fun compactHashMap_4() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(4, 4)
    assertEquals(4, m[4])
    m.trimToSize()
    assertEquals(4, m[4])
  }
  @Test fun compactHashMap_5() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(5, 5)
    assertEquals(5, m[5])
    m.trimToSize()
    assertEquals(5, m[5])
  }
  @Test fun compactHashMap_6() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(6, 6)
    assertEquals(6, m[6])
    m.trimToSize()
    assertEquals(6, m[6])
  }
  @Test fun compactHashMap_7() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(7, 7)
    assertEquals(7, m[7])
    m.trimToSize()
    assertEquals(7, m[7])
  }
  @Test fun compactHashMap_8() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(8, 8)
    assertEquals(8, m[8])
    m.trimToSize()
    assertEquals(8, m[8])
  }
  @Test fun compactHashMap_9() {
    val m = CompactHashMap.create<Int, Int>()
    m.put(9, 9)
    assertEquals(9, m[9])
    m.trimToSize()
    assertEquals(9, m[9])
  }
  @Test fun treeMap_order_seed_0() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 0 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_1() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 1 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_2() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 2 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_3() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 3 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_4() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 4 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_5() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 5 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_6() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 6 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_7() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 7 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_8() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 8 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
  @Test fun treeMap_order_seed_9() {
    val m = Maps.newTreeMap<Int, Int>()
    for (x in listOf(3,1,2).map { it + 9 }) m[x] = x
    assertEquals(m.keys.toList(), m.keys.toList().sorted())
  }
}