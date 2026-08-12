package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterizationHashBatchTest {
  @Test fun murmur3_32_bytes_0() {
    val b = ByteArray(0) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_1() {
    val b = ByteArray(1) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_2() {
    val b = ByteArray(2) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_3() {
    val b = ByteArray(3) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_4() {
    val b = ByteArray(4) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_5() {
    val b = ByteArray(5) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_6() {
    val b = ByteArray(6) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_7() {
    val b = ByteArray(7) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_8() {
    val b = ByteArray(8) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_9() {
    val b = ByteArray(9) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_10() {
    val b = ByteArray(10) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_11() {
    val b = ByteArray(11) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_12() {
    val b = ByteArray(12) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_13() {
    val b = ByteArray(13) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_14() {
    val b = ByteArray(14) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_15() {
    val b = ByteArray(15) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_16() {
    val b = ByteArray(16) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_17() {
    val b = ByteArray(17) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_18() {
    val b = ByteArray(18) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_19() {
    val b = ByteArray(19) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_20() {
    val b = ByteArray(20) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_21() {
    val b = ByteArray(21) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_22() {
    val b = ByteArray(22) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_23() {
    val b = ByteArray(23) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_24() {
    val b = ByteArray(24) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_25() {
    val b = ByteArray(25) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_26() {
    val b = ByteArray(26) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_27() {
    val b = ByteArray(27) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_28() {
    val b = ByteArray(28) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_29() {
    val b = ByteArray(29) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_30() {
    val b = ByteArray(30) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_31() {
    val b = ByteArray(31) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_32() {
    val b = ByteArray(32) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_33() {
    val b = ByteArray(33) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_34() {
    val b = ByteArray(34) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_35() {
    val b = ByteArray(35) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_36() {
    val b = ByteArray(36) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_37() {
    val b = ByteArray(37) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_38() {
    val b = ByteArray(38) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun murmur3_32_bytes_39() {
    val b = ByteArray(39) { it.toByte() }
    val h = Hashing.murmur3_32().hashBytes(b).asInt()
    assertEquals(h, Hashing.murmur3_32().hashBytes(b).asInt())
  }
  @Test fun sha256_len_0() {
    val h = Hashing.sha256().hashBytes(ByteArray(0)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_1() {
    val h = Hashing.sha256().hashBytes(ByteArray(1)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_2() {
    val h = Hashing.sha256().hashBytes(ByteArray(2)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_3() {
    val h = Hashing.sha256().hashBytes(ByteArray(3)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_4() {
    val h = Hashing.sha256().hashBytes(ByteArray(4)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_5() {
    val h = Hashing.sha256().hashBytes(ByteArray(5)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_6() {
    val h = Hashing.sha256().hashBytes(ByteArray(6)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_7() {
    val h = Hashing.sha256().hashBytes(ByteArray(7)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_8() {
    val h = Hashing.sha256().hashBytes(ByteArray(8)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_9() {
    val h = Hashing.sha256().hashBytes(ByteArray(9)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_10() {
    val h = Hashing.sha256().hashBytes(ByteArray(10)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_11() {
    val h = Hashing.sha256().hashBytes(ByteArray(11)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_12() {
    val h = Hashing.sha256().hashBytes(ByteArray(12)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_13() {
    val h = Hashing.sha256().hashBytes(ByteArray(13)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_14() {
    val h = Hashing.sha256().hashBytes(ByteArray(14)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_15() {
    val h = Hashing.sha256().hashBytes(ByteArray(15)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_16() {
    val h = Hashing.sha256().hashBytes(ByteArray(16)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_17() {
    val h = Hashing.sha256().hashBytes(ByteArray(17)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_18() {
    val h = Hashing.sha256().hashBytes(ByteArray(18)).asBytes()
    assertEquals(32, h.size)
  }
  @Test fun sha256_len_19() {
    val h = Hashing.sha256().hashBytes(ByteArray(19)).asBytes()
    assertEquals(32, h.size)
  }
}