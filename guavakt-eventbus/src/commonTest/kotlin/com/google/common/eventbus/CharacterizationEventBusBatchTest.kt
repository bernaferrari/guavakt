package dev.guavakt.eventbus

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterizationEventBusBatchTest {
  @Test fun post_0() {
    val bus = EventBus("t0")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(0)
    assertEquals(0, got)
  }
  @Test fun post_1() {
    val bus = EventBus("t1")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(1)
    assertEquals(1, got)
  }
  @Test fun post_2() {
    val bus = EventBus("t2")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(2)
    assertEquals(2, got)
  }
  @Test fun post_3() {
    val bus = EventBus("t3")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(3)
    assertEquals(3, got)
  }
  @Test fun post_4() {
    val bus = EventBus("t4")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(4)
    assertEquals(4, got)
  }
  @Test fun post_5() {
    val bus = EventBus("t5")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(5)
    assertEquals(5, got)
  }
  @Test fun post_6() {
    val bus = EventBus("t6")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(6)
    assertEquals(6, got)
  }
  @Test fun post_7() {
    val bus = EventBus("t7")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(7)
    assertEquals(7, got)
  }
  @Test fun post_8() {
    val bus = EventBus("t8")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(8)
    assertEquals(8, got)
  }
  @Test fun post_9() {
    val bus = EventBus("t9")
    var got = -1
    val sub = Any()
    bus.registerHandler<Int>(sub) { got = it }
    bus.post(9)
    assertEquals(9, got)
  }
}