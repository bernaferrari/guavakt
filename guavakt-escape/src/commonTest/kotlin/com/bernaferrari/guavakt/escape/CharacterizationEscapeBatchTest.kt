package com.bernaferrari.guavakt.escape

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterizationEscapeBatchTest {
  @Test fun identity_escape_0() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x0", e.escape("x0"))
  }
  @Test fun identity_escape_1() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x1", e.escape("x1"))
  }
  @Test fun identity_escape_2() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x2", e.escape("x2"))
  }
  @Test fun identity_escape_3() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x3", e.escape("x3"))
  }
  @Test fun identity_escape_4() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x4", e.escape("x4"))
  }
  @Test fun identity_escape_5() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x5", e.escape("x5"))
  }
  @Test fun identity_escape_6() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x6", e.escape("x6"))
  }
  @Test fun identity_escape_7() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x7", e.escape("x7"))
  }
  @Test fun identity_escape_8() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x8", e.escape("x8"))
  }
  @Test fun identity_escape_9() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x9", e.escape("x9"))
  }
  @Test fun identity_escape_10() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x10", e.escape("x10"))
  }
  @Test fun identity_escape_11() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x11", e.escape("x11"))
  }
  @Test fun identity_escape_12() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x12", e.escape("x12"))
  }
  @Test fun identity_escape_13() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x13", e.escape("x13"))
  }
  @Test fun identity_escape_14() {
    val e = object : CharEscaper() {
      override fun escape(c: Char): CharArray? = null
    }
    assertEquals("x14", e.escape("x14"))
  }
}