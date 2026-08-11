package dev.guavakt.parity

import com.google.common.math.Quantiles as GuavaQuantiles
import dev.guavakt.math.Quantiles
import kotlin.test.Test
import kotlin.test.assertEquals

class QuantilesDifferentialTest {
    @Test
    fun singleQuantilesMatchGuavaForUnsortedFiniteAndInfiniteDatasets() {
        val datasets = listOf(
            doubleArrayOf(9.0, -4.0, 0.0, 1.0, 3.0, 7.0, 8.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(Double.NEGATIVE_INFINITY, 4.0),
            doubleArrayOf(4.0, Double.POSITIVE_INFINITY),
            doubleArrayOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
            doubleArrayOf(Double.NaN, 1.0, 2.0),
        )
        for (dataset in datasets) {
            for (scale in listOf(2, 4, 7, 100)) {
                for (index in 0..scale) {
                    assertEquals(
                        GuavaQuantiles.scale(scale).index(index).compute(*dataset).toRawBits(),
                        Quantiles.scale(scale).index(index).compute(dataset).toRawBits(),
                        "dataset=${dataset.contentToString()} scale=$scale index=$index",
                    )
                }
            }
        }
    }

    @Test
    fun multiIndexAndInPlaceComputationsMatchGuava() {
        val indexes = intArrayOf(0, 1, 3, 4, 7)
        val source = doubleArrayOf(17.0, -2.0, 5.0, 5.0, 100.0, 0.0)
        val guavaInput = source.copyOf()
        val ourInput = source.copyOf()

        val guava = GuavaQuantiles.scale(7).indexes(*indexes).computeInPlace(*guavaInput)
        val ours = Quantiles.scale(7).indexes(*indexes).computeInPlace(ourInput)

        assertEquals(
            guava.entries.map { it.key to it.value.toRawBits() },
            ours.entries.map { it.key to it.value.toRawBits() },
        )
        assertEquals(guavaInput.toList(), ourInput.toList())
    }

    @Test
    fun validationFailuresMatchGuava() {
        assertEquals(
            outcome { GuavaQuantiles.scale(0) },
            outcome { Quantiles.scale(0) },
        )
        assertEquals(
            outcome { GuavaQuantiles.scale(4).index(5) },
            outcome { Quantiles.scale(4).index(5) },
        )
        assertEquals(
            outcome { GuavaQuantiles.scale(4).indexes().compute(*doubleArrayOf()) },
            outcome { Quantiles.scale(4).indexes().compute(doubleArrayOf(1.0)) },
        )
        assertEquals(
            outcome { GuavaQuantiles.median().compute(*doubleArrayOf()) },
            outcome { Quantiles.median().compute(doubleArrayOf()) },
        )
    }

    private fun <T> outcome(action: () -> T): String = try {
        action()
        "value"
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
