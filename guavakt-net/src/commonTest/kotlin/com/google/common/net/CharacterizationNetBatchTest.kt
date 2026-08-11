package dev.guavakt.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterizationNetBatchTest {
  @Test fun hostAndPort_string_0() {
    val hp = HostAndPort.fromParts("localhost", 1)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_1() {
    val hp = HostAndPort.fromParts("localhost", 2)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_2() {
    val hp = HostAndPort.fromParts("localhost", 3)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_3() {
    val hp = HostAndPort.fromParts("localhost", 4)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_4() {
    val hp = HostAndPort.fromParts("localhost", 5)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_5() {
    val hp = HostAndPort.fromParts("localhost", 6)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_6() {
    val hp = HostAndPort.fromParts("localhost", 7)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_7() {
    val hp = HostAndPort.fromParts("localhost", 8)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_8() {
    val hp = HostAndPort.fromParts("localhost", 9)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_9() {
    val hp = HostAndPort.fromParts("localhost", 10)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_10() {
    val hp = HostAndPort.fromParts("localhost", 11)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_11() {
    val hp = HostAndPort.fromParts("localhost", 12)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_12() {
    val hp = HostAndPort.fromParts("localhost", 13)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_13() {
    val hp = HostAndPort.fromParts("localhost", 14)
    assertTrue(hp.toString().contains("localhost"))
  }
  @Test fun hostAndPort_string_14() {
    val hp = HostAndPort.fromParts("localhost", 15)
    assertTrue(hp.toString().contains("localhost"))
  }
}