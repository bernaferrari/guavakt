package com.bernaferrari.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CharacterizationBaseBatchTest {
  @Test fun checkArgument_ok_0() { Preconditions.checkArgument(true, "0") }
  @Test fun checkArgument_ok_1() { Preconditions.checkArgument(true, "1") }
  @Test fun checkArgument_ok_2() { Preconditions.checkArgument(true, "2") }
  @Test fun checkArgument_ok_3() { Preconditions.checkArgument(true, "3") }
  @Test fun checkArgument_ok_4() { Preconditions.checkArgument(true, "4") }
  @Test fun checkArgument_ok_5() { Preconditions.checkArgument(true, "5") }
  @Test fun checkArgument_ok_6() { Preconditions.checkArgument(true, "6") }
  @Test fun checkArgument_ok_7() { Preconditions.checkArgument(true, "7") }
  @Test fun checkArgument_ok_8() { Preconditions.checkArgument(true, "8") }
  @Test fun checkArgument_ok_9() { Preconditions.checkArgument(true, "9") }
  @Test fun checkArgument_ok_10() { Preconditions.checkArgument(true, "10") }
  @Test fun checkArgument_ok_11() { Preconditions.checkArgument(true, "11") }
  @Test fun checkArgument_ok_12() { Preconditions.checkArgument(true, "12") }
  @Test fun checkArgument_ok_13() { Preconditions.checkArgument(true, "13") }
  @Test fun checkArgument_ok_14() { Preconditions.checkArgument(true, "14") }
  @Test fun checkArgument_ok_15() { Preconditions.checkArgument(true, "15") }
  @Test fun checkArgument_ok_16() { Preconditions.checkArgument(true, "16") }
  @Test fun checkArgument_ok_17() { Preconditions.checkArgument(true, "17") }
  @Test fun checkArgument_ok_18() { Preconditions.checkArgument(true, "18") }
  @Test fun checkArgument_ok_19() { Preconditions.checkArgument(true, "19") }
  @Test fun checkArgument_ok_20() { Preconditions.checkArgument(true, "20") }
  @Test fun checkArgument_ok_21() { Preconditions.checkArgument(true, "21") }
  @Test fun checkArgument_ok_22() { Preconditions.checkArgument(true, "22") }
  @Test fun checkArgument_ok_23() { Preconditions.checkArgument(true, "23") }
  @Test fun checkArgument_ok_24() { Preconditions.checkArgument(true, "24") }
  @Test fun checkArgument_ok_25() { Preconditions.checkArgument(true, "25") }
  @Test fun checkArgument_ok_26() { Preconditions.checkArgument(true, "26") }
  @Test fun checkArgument_ok_27() { Preconditions.checkArgument(true, "27") }
  @Test fun checkArgument_ok_28() { Preconditions.checkArgument(true, "28") }
  @Test fun checkArgument_ok_29() { Preconditions.checkArgument(true, "29") }
  @Test fun checkArgument_fail_0() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "0") } }
  @Test fun checkArgument_fail_1() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "1") } }
  @Test fun checkArgument_fail_2() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "2") } }
  @Test fun checkArgument_fail_3() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "3") } }
  @Test fun checkArgument_fail_4() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "4") } }
  @Test fun checkArgument_fail_5() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "5") } }
  @Test fun checkArgument_fail_6() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "6") } }
  @Test fun checkArgument_fail_7() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "7") } }
  @Test fun checkArgument_fail_8() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "8") } }
  @Test fun checkArgument_fail_9() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "9") } }
  @Test fun checkArgument_fail_10() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "10") } }
  @Test fun checkArgument_fail_11() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "11") } }
  @Test fun checkArgument_fail_12() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "12") } }
  @Test fun checkArgument_fail_13() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "13") } }
  @Test fun checkArgument_fail_14() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "14") } }
  @Test fun checkArgument_fail_15() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "15") } }
  @Test fun checkArgument_fail_16() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "16") } }
  @Test fun checkArgument_fail_17() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "17") } }
  @Test fun checkArgument_fail_18() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "18") } }
  @Test fun checkArgument_fail_19() { assertFailsWith<IllegalArgumentException> { Preconditions.checkArgument(false, "19") } }
  @Test fun joiner_0() { assertEquals("0", Joiner.on(".").join(listOf("0"))) }
  @Test fun joiner_1() { assertEquals("0.1", Joiner.on(".").join(listOf("0", "1"))) }
  @Test fun joiner_2() { assertEquals("0.1.2", Joiner.on(".").join(listOf("0", "1", "2"))) }
  @Test fun joiner_3() { assertEquals("0.1.2.3", Joiner.on(".").join(listOf("0", "1", "2", "3"))) }
  @Test fun joiner_4() { assertEquals("0.1.2.3.4", Joiner.on(".").join(listOf("0", "1", "2", "3", "4"))) }
  @Test fun joiner_5() { assertEquals("0.1.2.3.4.5", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5"))) }
  @Test fun joiner_6() { assertEquals("0.1.2.3.4.5.6", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6"))) }
  @Test fun joiner_7() { assertEquals("0.1.2.3.4.5.6.7", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7"))) }
  @Test fun joiner_8() { assertEquals("0.1.2.3.4.5.6.7.8", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8"))) }
  @Test fun joiner_9() { assertEquals("0.1.2.3.4.5.6.7.8.9", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"))) }
  @Test fun joiner_10() { assertEquals("0.1.2.3.4.5.6.7.8.9.10", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))) }
  @Test fun joiner_11() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))) }
  @Test fun joiner_12() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"))) }
  @Test fun joiner_13() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13"))) }
  @Test fun joiner_14() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13.14", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"))) }
  @Test fun joiner_15() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13.14.15", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"))) }
  @Test fun joiner_16() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13.14.15.16", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"))) }
  @Test fun joiner_17() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13.14.15.16.17", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"))) }
  @Test fun joiner_18() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13.14.15.16.17.18", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18"))) }
  @Test fun joiner_19() { assertEquals("0.1.2.3.4.5.6.7.8.9.10.11.12.13.14.15.16.17.18.19", Joiner.on(".").join(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19"))) }
  @Test fun suppliers_ofInstance_0() { assertEquals(0, Suppliers.ofInstance(0).get()) }
  @Test fun suppliers_ofInstance_1() { assertEquals(1, Suppliers.ofInstance(1).get()) }
  @Test fun suppliers_ofInstance_2() { assertEquals(2, Suppliers.ofInstance(2).get()) }
  @Test fun suppliers_ofInstance_3() { assertEquals(3, Suppliers.ofInstance(3).get()) }
  @Test fun suppliers_ofInstance_4() { assertEquals(4, Suppliers.ofInstance(4).get()) }
  @Test fun suppliers_ofInstance_5() { assertEquals(5, Suppliers.ofInstance(5).get()) }
  @Test fun suppliers_ofInstance_6() { assertEquals(6, Suppliers.ofInstance(6).get()) }
  @Test fun suppliers_ofInstance_7() { assertEquals(7, Suppliers.ofInstance(7).get()) }
  @Test fun suppliers_ofInstance_8() { assertEquals(8, Suppliers.ofInstance(8).get()) }
  @Test fun suppliers_ofInstance_9() { assertEquals(9, Suppliers.ofInstance(9).get()) }
  @Test fun suppliers_ofInstance_10() { assertEquals(10, Suppliers.ofInstance(10).get()) }
  @Test fun suppliers_ofInstance_11() { assertEquals(11, Suppliers.ofInstance(11).get()) }
  @Test fun suppliers_ofInstance_12() { assertEquals(12, Suppliers.ofInstance(12).get()) }
  @Test fun suppliers_ofInstance_13() { assertEquals(13, Suppliers.ofInstance(13).get()) }
  @Test fun suppliers_ofInstance_14() { assertEquals(14, Suppliers.ofInstance(14).get()) }
  @Test fun optional_of_0() { assertEquals(0, Optional.of(0).get()) }
  @Test fun optional_of_1() { assertEquals(1, Optional.of(1).get()) }
  @Test fun optional_of_2() { assertEquals(2, Optional.of(2).get()) }
  @Test fun optional_of_3() { assertEquals(3, Optional.of(3).get()) }
  @Test fun optional_of_4() { assertEquals(4, Optional.of(4).get()) }
  @Test fun optional_of_5() { assertEquals(5, Optional.of(5).get()) }
  @Test fun optional_of_6() { assertEquals(6, Optional.of(6).get()) }
  @Test fun optional_of_7() { assertEquals(7, Optional.of(7).get()) }
  @Test fun optional_of_8() { assertEquals(8, Optional.of(8).get()) }
  @Test fun optional_of_9() { assertEquals(9, Optional.of(9).get()) }
  @Test fun optional_of_10() { assertEquals(10, Optional.of(10).get()) }
  @Test fun optional_of_11() { assertEquals(11, Optional.of(11).get()) }
  @Test fun optional_of_12() { assertEquals(12, Optional.of(12).get()) }
  @Test fun optional_of_13() { assertEquals(13, Optional.of(13).get()) }
  @Test fun optional_of_14() { assertEquals(14, Optional.of(14).get()) }
}