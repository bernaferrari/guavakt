package com.bernaferrari.guavakt.io

import kotlin.test.Test
import kotlin.test.assertEquals

class PathSimplificationTest {
    @Test fun absolutePathsDoNotEscapeAboveRoot() {
        assertEquals("/a", Files.simplifyPath("/../../a"))
        assertEquals("/", Files.simplifyPath("/.."))
        assertEquals("../../a", Files.simplifyPath("../../a"))
    }
}
