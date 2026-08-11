package dev.guavakt.parity

import com.google.common.hash.BloomFilter as GuavaBloomFilter
import com.google.common.hash.Funnel as GuavaFunnel
import com.google.common.hash.Funnels as GuavaFunnels
import com.google.common.hash.Hashing as GuavaHashing
import com.google.common.hash.PrimitiveSink as GuavaPrimitiveSink
import dev.guavakt.hash.BloomFilter as GuavaKtBloomFilter
import dev.guavakt.hash.Funnel as GuavaKtFunnel
import dev.guavakt.hash.Funnels as GuavaKtFunnels
import dev.guavakt.hash.Hashing as GuavaKtHashing
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class BloomFilterDifferentialTest {
    @Test
    fun murmur128VectorsMatchTheGuavaHashUsedByBloomFilters() {
        val byteVectors = listOf(
            byteArrayOf(),
            byteArrayOf(0),
            ByteArray(15) { it.toByte() },
            ByteArray(16) { it.toByte() },
            ByteArray(17) { it.toByte() },
            ByteArray(257) { (it * 31).toByte() },
        )
        assertEquals(
            byteVectors.map { GuavaHashing.murmur3_128().hashBytes(it).toString() },
            byteVectors.map { GuavaKtHashing.murmur3_128().hashBytes(it).toString() },
        )
        assertEquals(
            listOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)
                .map { GuavaHashing.murmur3_128().hashInt(it).toString() },
            listOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)
                .map { GuavaKtHashing.murmur3_128().hashInt(it).toString() },
        )
    }

    @Test
    fun integerStrategyProducesByteIdenticalFilters() {
        val guava = GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), 100, 0.01)
        val guavaKt = GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), 100, 0.01)
        val guavaPuts = mutableListOf<Boolean>()
        val guavaKtPuts = mutableListOf<Boolean>()
        listOf(0, 1, 2, 17, -1, Int.MAX_VALUE, Int.MIN_VALUE, 17).forEach {
            guavaPuts += guava.put(it)
            guavaKtPuts += guavaKt.put(it)
        }

        assertEquals(guavaPuts, guavaKtPuts)
        assertEquals(write(guava).toList(), guavaKt.toByteArray().toList())
        assertEquals(
            (-40..40).map(guava::mightContain),
            (-40..40).map(guavaKt::mightContain),
        )
        assertEquals(guava.expectedFpp(), guavaKt.expectedFpp())
        assertEquals(guava.approximateElementCount(), guavaKt.approximateElementCount())
        assertEquals(guava.serializedSize(), guavaKt.serializedSize())
    }

    @Test
    fun unencodedCharacterStrategyAndUnicodeEdgesMatch() {
        val guava = GuavaBloomFilter.create(GuavaFunnels.unencodedCharsFunnel(), 75, 0.005)
        val guavaKt = GuavaKtBloomFilter.create(GuavaKtFunnels.unencodedCharsFunnel(), 75, 0.005)
        val inserted = listOf<CharSequence>("", "ascii", "café", "\u0000", "😀", "a😀z")
        inserted.forEach { value ->
            assertEquals(guava.put(value), guavaKt.put(value))
        }
        assertEquals(write(guava).toList(), guavaKt.toByteArray().toList())
        val candidates = inserted + listOf("missing", "CAFÉ", "😁")
        assertEquals(candidates.map(guava::mightContain), candidates.map(guavaKt::mightContain))
    }

    @Test
    fun customObjectFunnelMixedPrimitiveLayoutMatchesGuava() {
        val guava = GuavaBloomFilter.create(guavaPacketFunnel, 128, 0.01)
        val guavaKt = GuavaKtBloomFilter.create(guavaKtPacketFunnel, 128, 0.01)
        val inserted = listOf(
            Packet(0, 0, 0, "", byteArrayOf()),
            Packet(1, -1, Long.MIN_VALUE, "café", byteArrayOf(0, -1, 17)),
            Packet(-128, Int.MAX_VALUE, Long.MAX_VALUE, "a😀z", ByteArray(32) { (it * 13).toByte() }),
            Packet(7, Int.MIN_VALUE, -9, "\u0000", byteArrayOf(42)),
        )
        assertEquals(inserted.map(guava::put), inserted.map(guavaKt::put))
        assertEquals(write(guava).toList(), guavaKt.toByteArray().toList())

        val candidates = inserted + listOf(
            Packet(1, -1, Long.MIN_VALUE, "cafe", byteArrayOf(0, -1, 17)),
            Packet(1, -1, Long.MIN_VALUE, "café", byteArrayOf(0, -1, 18)),
        )
        assertEquals(candidates.map(guava::mightContain), candidates.map(guavaKt::mightContain))

        val guavaKtFromGuava = GuavaKtBloomFilter.readFrom(write(guava), guavaKtPacketFunnel)
        val guavaFromGuavaKt = GuavaBloomFilter.readFrom(
            ByteArrayInputStream(guavaKt.toByteArray()),
            guavaPacketFunnel,
        )
        assertEquals(write(guava).toList(), guavaKtFromGuava.toByteArray().toList())
        assertEquals(write(guavaFromGuavaKt).toList(), guavaKt.toByteArray().toList())
    }

    @Test
    fun copyCompatibilityUnionEqualityAndFailureRulesMatch() {
        val guavaFirst = GuavaBloomFilter.create(GuavaFunnels.longFunnel(), 200, 0.01)
        val guavaSecond = GuavaBloomFilter.create(GuavaFunnels.longFunnel(), 200, 0.01)
        val guavaKtFirst = GuavaKtBloomFilter.create(GuavaKtFunnels.longFunnel(), 200, 0.01)
        val guavaKtSecond = GuavaKtBloomFilter.create(GuavaKtFunnels.longFunnel(), 200, 0.01)
        (0L until 40L step 2L).forEach { guavaFirst.put(it); guavaKtFirst.put(it) }
        (1L until 40L step 2L).forEach { guavaSecond.put(it); guavaKtSecond.put(it) }
        val guavaCopy = guavaFirst.copy()
        val guavaKtCopy = guavaKtFirst.copy()

        assertEquals(
            listOf(
                guavaFirst == guavaCopy,
                guavaFirst.isCompatible(guavaSecond),
                guavaFirst.isCompatible(guavaFirst),
                failureName { guavaFirst.putAll(guavaFirst) },
            ),
            listOf(
                guavaKtFirst == guavaKtCopy,
                guavaKtFirst.isCompatible(guavaKtSecond),
                guavaKtFirst.isCompatible(guavaKtFirst),
                failureName { guavaKtFirst.putAll(guavaKtFirst) },
            ),
        )

        guavaFirst.putAll(guavaSecond)
        guavaKtFirst.putAll(guavaKtSecond)
        assertEquals(write(guavaFirst).toList(), guavaKtFirst.toByteArray().toList())
        assertEquals((0L until 40L).map(guavaFirst::mightContain), (0L until 40L).map(guavaKtFirst::mightContain))
    }

    @Test
    fun wireFormatsRoundTripAcrossImplementations() {
        val guava = GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), 300, 0.02)
        (0..90).forEach(guava::put)
        val guavaBytes = write(guava)
        val guavaKtFromGuava = GuavaKtBloomFilter.readFrom(guavaBytes, GuavaKtFunnels.integerFunnel())
        assertEquals(guavaBytes.toList(), guavaKtFromGuava.toByteArray().toList())

        val guavaKt = GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), 300, 0.02)
        (0..90).forEach(guavaKt::put)
        val guavaFromGuavaKt = GuavaBloomFilter.readFrom(
            ByteArrayInputStream(guavaKt.toByteArray()),
            GuavaFunnels.integerFunnel(),
        )
        assertEquals(write(guavaFromGuavaKt).toList(), guavaKt.toByteArray().toList())
    }

    @Test
    fun factoryValidationAndSizingMatch() {
        val guavaFailures = listOf(
            failureName { GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), -1) },
            failureName { GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), 1, 0.0) },
            failureName { GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), 1, 1.0) },
            failureName { GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), 1, Double.NaN) },
        )
        val guavaKtFailures = listOf(
            failureName { GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), -1) },
            failureName { GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), 1, 0.0) },
            failureName { GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), 1, 1.0) },
            failureName { GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), 1, Double.NaN) },
        )
        assertEquals(guavaFailures, guavaKtFailures)

        val shapes = listOf(0L to 0.03, 1L to 0.03, 100L to 0.01, 1_000L to 0.01, 13L to 0.9)
        assertEquals(
            shapes.map { (n, fpp) -> GuavaBloomFilter.create(GuavaFunnels.integerFunnel(), n, fpp).serializedSize() },
            shapes.map { (n, fpp) -> GuavaKtBloomFilter.create(GuavaKtFunnels.integerFunnel(), n, fpp).serializedSize() },
        )
    }

    private fun <T : Any> write(filter: GuavaBloomFilter<T>): ByteArray =
        ByteArrayOutputStream().also(filter::writeTo).toByteArray()

    private data class Packet(
        val kind: Byte,
        val id: Int,
        val sequence: Long,
        val label: String,
        val payload: ByteArray,
    )

    private val guavaPacketFunnel = object : GuavaFunnel<Packet> {
        override fun funnel(from: Packet, into: GuavaPrimitiveSink) {
            into.putByte(from.kind)
                .putInt(from.id)
                .putLong(from.sequence)
                .putUnencodedChars(from.label)
                .putBytes(from.payload)
        }
    }

    private val guavaKtPacketFunnel = GuavaKtFunnel<Packet> { from, into ->
        into.putByte(from.kind)
            .putInt(from.id)
            .putLong(from.sequence)
            .putUnencodedChars(from.label)
            .putBytes(from.payload)
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
