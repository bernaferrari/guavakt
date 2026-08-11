package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterizationConcurrentBatchTest {
  @Test fun immediateFuture_0() { assertEquals(0, Futures.immediateFuture(0).get()) }
  @Test fun immediateFuture_1() { assertEquals(1, Futures.immediateFuture(1).get()) }
  @Test fun immediateFuture_2() { assertEquals(2, Futures.immediateFuture(2).get()) }
  @Test fun immediateFuture_3() { assertEquals(3, Futures.immediateFuture(3).get()) }
  @Test fun immediateFuture_4() { assertEquals(4, Futures.immediateFuture(4).get()) }
  @Test fun immediateFuture_5() { assertEquals(5, Futures.immediateFuture(5).get()) }
  @Test fun immediateFuture_6() { assertEquals(6, Futures.immediateFuture(6).get()) }
  @Test fun immediateFuture_7() { assertEquals(7, Futures.immediateFuture(7).get()) }
  @Test fun immediateFuture_8() { assertEquals(8, Futures.immediateFuture(8).get()) }
  @Test fun immediateFuture_9() { assertEquals(9, Futures.immediateFuture(9).get()) }
  @Test fun immediateFuture_10() { assertEquals(10, Futures.immediateFuture(10).get()) }
  @Test fun immediateFuture_11() { assertEquals(11, Futures.immediateFuture(11).get()) }
  @Test fun immediateFuture_12() { assertEquals(12, Futures.immediateFuture(12).get()) }
  @Test fun immediateFuture_13() { assertEquals(13, Futures.immediateFuture(13).get()) }
  @Test fun immediateFuture_14() { assertEquals(14, Futures.immediateFuture(14).get()) }
  @Test fun immediateFuture_15() { assertEquals(15, Futures.immediateFuture(15).get()) }
  @Test fun immediateFuture_16() { assertEquals(16, Futures.immediateFuture(16).get()) }
  @Test fun immediateFuture_17() { assertEquals(17, Futures.immediateFuture(17).get()) }
  @Test fun immediateFuture_18() { assertEquals(18, Futures.immediateFuture(18).get()) }
  @Test fun immediateFuture_19() { assertEquals(19, Futures.immediateFuture(19).get()) }
  @Test fun immediateFuture_20() { assertEquals(20, Futures.immediateFuture(20).get()) }
  @Test fun immediateFuture_21() { assertEquals(21, Futures.immediateFuture(21).get()) }
  @Test fun immediateFuture_22() { assertEquals(22, Futures.immediateFuture(22).get()) }
  @Test fun immediateFuture_23() { assertEquals(23, Futures.immediateFuture(23).get()) }
  @Test fun immediateFuture_24() { assertEquals(24, Futures.immediateFuture(24).get()) }
  @Test fun immediateFuture_25() { assertEquals(25, Futures.immediateFuture(25).get()) }
  @Test fun immediateFuture_26() { assertEquals(26, Futures.immediateFuture(26).get()) }
  @Test fun immediateFuture_27() { assertEquals(27, Futures.immediateFuture(27).get()) }
  @Test fun immediateFuture_28() { assertEquals(28, Futures.immediateFuture(28).get()) }
  @Test fun immediateFuture_29() { assertEquals(29, Futures.immediateFuture(29).get()) }
  @Test fun settableFuture_0() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(0))
    assertEquals(0, f.get())
  }
  @Test fun settableFuture_1() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(1))
    assertEquals(1, f.get())
  }
  @Test fun settableFuture_2() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(2))
    assertEquals(2, f.get())
  }
  @Test fun settableFuture_3() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(3))
    assertEquals(3, f.get())
  }
  @Test fun settableFuture_4() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(4))
    assertEquals(4, f.get())
  }
  @Test fun settableFuture_5() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(5))
    assertEquals(5, f.get())
  }
  @Test fun settableFuture_6() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(6))
    assertEquals(6, f.get())
  }
  @Test fun settableFuture_7() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(7))
    assertEquals(7, f.get())
  }
  @Test fun settableFuture_8() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(8))
    assertEquals(8, f.get())
  }
  @Test fun settableFuture_9() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(9))
    assertEquals(9, f.get())
  }
  @Test fun settableFuture_10() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(10))
    assertEquals(10, f.get())
  }
  @Test fun settableFuture_11() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(11))
    assertEquals(11, f.get())
  }
  @Test fun settableFuture_12() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(12))
    assertEquals(12, f.get())
  }
  @Test fun settableFuture_13() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(13))
    assertEquals(13, f.get())
  }
  @Test fun settableFuture_14() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(14))
    assertEquals(14, f.get())
  }
  @Test fun settableFuture_15() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(15))
    assertEquals(15, f.get())
  }
  @Test fun settableFuture_16() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(16))
    assertEquals(16, f.get())
  }
  @Test fun settableFuture_17() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(17))
    assertEquals(17, f.get())
  }
  @Test fun settableFuture_18() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(18))
    assertEquals(18, f.get())
  }
  @Test fun settableFuture_19() {
    val f = SettableFuture.create<Int>()
    assertTrue(f.set(19))
    assertEquals(19, f.get())
  }
  @Test fun directExecutor_runs_0() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 0 }
    assertEquals(0, x)
  }
  @Test fun directExecutor_runs_1() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 1 }
    assertEquals(1, x)
  }
  @Test fun directExecutor_runs_2() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 2 }
    assertEquals(2, x)
  }
  @Test fun directExecutor_runs_3() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 3 }
    assertEquals(3, x)
  }
  @Test fun directExecutor_runs_4() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 4 }
    assertEquals(4, x)
  }
  @Test fun directExecutor_runs_5() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 5 }
    assertEquals(5, x)
  }
  @Test fun directExecutor_runs_6() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 6 }
    assertEquals(6, x)
  }
  @Test fun directExecutor_runs_7() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 7 }
    assertEquals(7, x)
  }
  @Test fun directExecutor_runs_8() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 8 }
    assertEquals(8, x)
  }
  @Test fun directExecutor_runs_9() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 9 }
    assertEquals(9, x)
  }
  @Test fun directExecutor_runs_10() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 10 }
    assertEquals(10, x)
  }
  @Test fun directExecutor_runs_11() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 11 }
    assertEquals(11, x)
  }
  @Test fun directExecutor_runs_12() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 12 }
    assertEquals(12, x)
  }
  @Test fun directExecutor_runs_13() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 13 }
    assertEquals(13, x)
  }
  @Test fun directExecutor_runs_14() {
    var x = 0
    MoreExecutors.directExecutor().execute { x = 14 }
    assertEquals(14, x)
  }
}