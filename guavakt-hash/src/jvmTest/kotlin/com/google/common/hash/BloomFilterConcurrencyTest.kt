package dev.guavakt.hash

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

class BloomFilterConcurrencyTest {
    @Test
    fun concurrentWritersDoNotLoseBitsOrIntroduceFalseNegatives() {
        val filter = BloomFilter.create(Funnels.integerFunnel(), 2_000, 0.001)
        val writers = List(8) { worker ->
            thread(start = true, name = "bloom-writer-$worker") {
                val start = worker * 250
                repeat(250) { offset -> filter.put(start + offset) }
            }
        }
        writers.forEach(Thread::join)

        (0 until 2_000).forEach { value ->
            assertTrue(filter.mightContain(value), "concurrent put lost $value")
        }
    }
}
