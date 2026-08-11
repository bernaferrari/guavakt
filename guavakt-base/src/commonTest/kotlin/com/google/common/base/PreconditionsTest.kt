package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PreconditionsTest {
    @Test
    fun elementIndexMessage() {
        val ex = assertFailsWith<IndexOutOfBoundsException> {
            Preconditions.checkElementIndex(2, 2, "index")
        }
        assertEquals("index (2) must be less than size (2)", ex.message)
    }

    @Test
    fun positionIndexesMessage() {
        val ex = assertFailsWith<IndexOutOfBoundsException> {
            Preconditions.checkPositionIndexes(2, 1, 3)
        }
        assertEquals("end index (1) must not be less than start index (2)", ex.message)
    }

    @Test
    fun checkArgumentTemplate() {
        val ex = assertFailsWith<IllegalArgumentException> {
            Preconditions.checkArgument(false, "bad %s", "x")
        }
        assertEquals("bad x", ex.message)
    }

    @Test
    fun checkNotNull() {
        assertEquals("a", Preconditions.checkNotNull("a"))
        assertFailsWith<NullPointerException> { Preconditions.checkNotNull(null) }
    }
}
