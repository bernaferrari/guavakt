package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallablesExceptionsTest {
    @Test
    fun callables_returning() {
        assertEquals(42, Callables.returning(42)())
        assertEquals(7, Futures.getDone(Callables.asyncReturning(7)()))
    }

    @Test
    fun uncheckedExecutionExceptionRetainsCause() {
        val ex = UncheckedExecutionException("x", IllegalStateException("c"))
        assertEquals("x", ex.message)
        assertTrue(ex.cause is IllegalStateException)
    }
}
