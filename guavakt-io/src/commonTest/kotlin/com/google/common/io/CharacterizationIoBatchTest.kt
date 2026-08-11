package dev.guavakt.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterizationIoBatchTest {
  @Test fun byteSource_size_0() {
    val src = ByteSource.wrap(ByteArray(0) { 1 })
    assertEquals(0, src.read().size)
  }
  @Test fun byteSource_size_1() {
    val src = ByteSource.wrap(ByteArray(1) { 1 })
    assertEquals(1, src.read().size)
  }
  @Test fun byteSource_size_2() {
    val src = ByteSource.wrap(ByteArray(2) { 1 })
    assertEquals(2, src.read().size)
  }
  @Test fun byteSource_size_3() {
    val src = ByteSource.wrap(ByteArray(3) { 1 })
    assertEquals(3, src.read().size)
  }
  @Test fun byteSource_size_4() {
    val src = ByteSource.wrap(ByteArray(4) { 1 })
    assertEquals(4, src.read().size)
  }
  @Test fun byteSource_size_5() {
    val src = ByteSource.wrap(ByteArray(5) { 1 })
    assertEquals(5, src.read().size)
  }
  @Test fun byteSource_size_6() {
    val src = ByteSource.wrap(ByteArray(6) { 1 })
    assertEquals(6, src.read().size)
  }
  @Test fun byteSource_size_7() {
    val src = ByteSource.wrap(ByteArray(7) { 1 })
    assertEquals(7, src.read().size)
  }
  @Test fun byteSource_size_8() {
    val src = ByteSource.wrap(ByteArray(8) { 1 })
    assertEquals(8, src.read().size)
  }
  @Test fun byteSource_size_9() {
    val src = ByteSource.wrap(ByteArray(9) { 1 })
    assertEquals(9, src.read().size)
  }
  @Test fun byteSource_size_10() {
    val src = ByteSource.wrap(ByteArray(10) { 1 })
    assertEquals(10, src.read().size)
  }
  @Test fun byteSource_size_11() {
    val src = ByteSource.wrap(ByteArray(11) { 1 })
    assertEquals(11, src.read().size)
  }
  @Test fun byteSource_size_12() {
    val src = ByteSource.wrap(ByteArray(12) { 1 })
    assertEquals(12, src.read().size)
  }
  @Test fun byteSource_size_13() {
    val src = ByteSource.wrap(ByteArray(13) { 1 })
    assertEquals(13, src.read().size)
  }
  @Test fun byteSource_size_14() {
    val src = ByteSource.wrap(ByteArray(14) { 1 })
    assertEquals(14, src.read().size)
  }
}