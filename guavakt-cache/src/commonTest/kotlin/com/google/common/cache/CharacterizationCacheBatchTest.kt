package dev.guavakt.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterizationCacheBatchTest {
  @Test fun putGet_0() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(0, 0)
    assertEquals(0, c.getIfPresent(0))
  }
  @Test fun putGet_1() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(1, 10)
    assertEquals(10, c.getIfPresent(1))
  }
  @Test fun putGet_2() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(2, 20)
    assertEquals(20, c.getIfPresent(2))
  }
  @Test fun putGet_3() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(3, 30)
    assertEquals(30, c.getIfPresent(3))
  }
  @Test fun putGet_4() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(4, 40)
    assertEquals(40, c.getIfPresent(4))
  }
  @Test fun putGet_5() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(5, 50)
    assertEquals(50, c.getIfPresent(5))
  }
  @Test fun putGet_6() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(6, 60)
    assertEquals(60, c.getIfPresent(6))
  }
  @Test fun putGet_7() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(7, 70)
    assertEquals(70, c.getIfPresent(7))
  }
  @Test fun putGet_8() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(8, 80)
    assertEquals(80, c.getIfPresent(8))
  }
  @Test fun putGet_9() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(9, 90)
    assertEquals(90, c.getIfPresent(9))
  }
  @Test fun putGet_10() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(10, 100)
    assertEquals(100, c.getIfPresent(10))
  }
  @Test fun putGet_11() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(11, 110)
    assertEquals(110, c.getIfPresent(11))
  }
  @Test fun putGet_12() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(12, 120)
    assertEquals(120, c.getIfPresent(12))
  }
  @Test fun putGet_13() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(13, 130)
    assertEquals(130, c.getIfPresent(13))
  }
  @Test fun putGet_14() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(14, 140)
    assertEquals(140, c.getIfPresent(14))
  }
  @Test fun putGet_15() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(15, 150)
    assertEquals(150, c.getIfPresent(15))
  }
  @Test fun putGet_16() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(16, 160)
    assertEquals(160, c.getIfPresent(16))
  }
  @Test fun putGet_17() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(17, 170)
    assertEquals(170, c.getIfPresent(17))
  }
  @Test fun putGet_18() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(18, 180)
    assertEquals(180, c.getIfPresent(18))
  }
  @Test fun putGet_19() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(19, 190)
    assertEquals(190, c.getIfPresent(19))
  }
  @Test fun putGet_20() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(20, 200)
    assertEquals(200, c.getIfPresent(20))
  }
  @Test fun putGet_21() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(21, 210)
    assertEquals(210, c.getIfPresent(21))
  }
  @Test fun putGet_22() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(22, 220)
    assertEquals(220, c.getIfPresent(22))
  }
  @Test fun putGet_23() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(23, 230)
    assertEquals(230, c.getIfPresent(23))
  }
  @Test fun putGet_24() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(24, 240)
    assertEquals(240, c.getIfPresent(24))
  }
  @Test fun putGet_25() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(25, 250)
    assertEquals(250, c.getIfPresent(25))
  }
  @Test fun putGet_26() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(26, 260)
    assertEquals(260, c.getIfPresent(26))
  }
  @Test fun putGet_27() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(27, 270)
    assertEquals(270, c.getIfPresent(27))
  }
  @Test fun putGet_28() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(28, 280)
    assertEquals(280, c.getIfPresent(28))
  }
  @Test fun putGet_29() {
    val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(100).build<Int, Int>()
    c.put(29, 290)
    assertEquals(290, c.getIfPresent(29))
  }
}