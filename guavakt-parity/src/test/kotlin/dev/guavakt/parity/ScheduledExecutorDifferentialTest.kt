package dev.guavakt.parity

import com.google.common.util.concurrent.MoreExecutors as GuavaMoreExecutors
import dev.guavakt.util.concurrent.WrappingScheduledExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ScheduledExecutorDifferentialTest {
    @Test
    fun oneShotValueAndPendingCancellation_matchGuava() {
        val guavaBackingExecutor = Executors.newSingleThreadScheduledExecutor()
        val guavaExecutor = GuavaMoreExecutors.listeningDecorator(guavaBackingExecutor)
        val guavaOneShot = guavaExecutor.schedule(Callable { 42 }, 1, TimeUnit.MILLISECONDS)
        val guavaPending = guavaExecutor.schedule(Callable { error("cancelled") }, 30, TimeUnit.SECONDS)

        val guavaKtExecutor = WrappingScheduledExecutorService()
        val guavaKtOneShot = guavaKtExecutor.schedule(kotlin.time.Duration.ZERO) { 42 }
        val guavaKtPending = guavaKtExecutor.schedule(30.seconds) { error("cancelled") }

        try {
            assertEquals(guavaOneShot.get(), guavaKtOneShot.get())
            assertEquals(guavaPending.cancel(false), guavaKtPending.cancel(false))
            assertTrue(guavaPending.isCancelled)
            assertTrue(guavaKtPending.isCancelled())
            assertFalse(guavaPending.cancel(false))
            assertFalse(guavaKtPending.cancel(false))
        } finally {
            guavaExecutor.shutdownNow()
            guavaKtExecutor.shutdown()
        }
    }
}
