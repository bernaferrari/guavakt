package dev.guavakt.hash

import dev.guavakt.base.Preconditions
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.max
import kotlin.math.pow

/**
 * A thread-safe, probabilistic set using Guava's Murmur3-128 Bloom strategy.
 *
 * False negatives are not introduced by [put], [putAll], or concurrent writes.
 * The portable wire API uses [ByteArray]; callers can pass those bytes through
 * Okio when streaming or filesystem storage is required.
 */
@OptIn(ExperimentalAtomicApi::class)
class BloomFilter<T> private constructor(
    words: LongArray,
    private val numHashFunctions: Int,
    private val funnel: Funnel<in T>,
    private val strategy: Strategy,
) {
    private val bits: Array<AtomicLong> = Array(words.size) { AtomicLong(words[it]) }
    private val bitSize: Long = words.size.toLong() * Long.SIZE_BITS

    init {
        require(words.isNotEmpty()) { "data length is zero" }
        require(numHashFunctions in 1..255) {
            "numHashFunctions ($numHashFunctions) must be between 1 and 255"
        }
    }

    /** Returns an independent snapshot with the same funnel and strategy. */
    fun copy(): BloomFilter<T> = BloomFilter(snapshot(), numHashFunctions, funnel, strategy)

    fun put(object_: T): Boolean {
        val hash = hash(object_)
        var changed = false
        strategy.forEachIndex(hash.first, hash.second, numHashFunctions, bitSize) { index ->
            changed = setBit(index) || changed
            true
        }
        return changed
    }

    fun mightContain(object_: T): Boolean {
        val hash = hash(object_)
        var present = true
        strategy.forEachIndex(hash.first, hash.second, numHashFunctions, bitSize) { index ->
            if (!getBit(index)) {
                present = false
                false
            } else {
                true
            }
        }
        return present
    }

    /** Guava Predicate-shaped alias. */
    fun apply(object_: T): Boolean = mightContain(object_)

    /** Java/Kotlin predicate-shaped alias. */
    fun test(object_: T): Boolean = mightContain(object_)

    operator fun invoke(object_: T): Boolean = mightContain(object_)

    /** Returns the current probability that a fresh value is reported present. */
    fun expectedFpp(): Double = (bitCount().toDouble() / bitSize.toDouble()).pow(numHashFunctions)

    /** Estimates inserted cardinality using Guava's half-up rounding rule. */
    fun approximateElementCount(): Long {
        val fraction = bitCount().toDouble() / bitSize.toDouble()
        val estimate = -ln1p(-fraction) * bitSize.toDouble() / numHashFunctions.toDouble()
        if (!estimate.isFinite() || estimate > Long.MAX_VALUE.toDouble()) {
            throw ArithmeticException("BloomFilter cardinality estimate is not a finite Long")
        }
        return floor(estimate + 0.5).toLong()
    }

    /** Bytes in the Guava wire shape: strategy, hash count, word count, then words. */
    fun serializedSize(): Long = bits.size.toLong() * Long.SIZE_BYTES + WIRE_HEADER_BYTES

    /** Whether [other] can be merged into this filter. A filter is not compatible with itself. */
    fun isCompatible(other: BloomFilter<T>): Boolean =
        other !== this &&
            numHashFunctions == other.numHashFunctions &&
            bitSize == other.bitSize &&
            strategy == other.strategy &&
            funnel == other.funnel

    /** Atomically unions every bit from a distinct compatible filter. */
    fun putAll(other: BloomFilter<T>) {
        require(other !== this) { "Cannot combine a BloomFilter with itself." }
        require(numHashFunctions == other.numHashFunctions) {
            "BloomFilters must have the same number of hash functions"
        }
        require(bitSize == other.bitSize) {
            "BloomFilters must have the same size underlying bit arrays"
        }
        require(strategy == other.strategy) { "BloomFilters must have equal strategies" }
        require(funnel == other.funnel) { "BloomFilters must have equal funnels" }
        val incoming = other.snapshot()
        for (index in bits.indices) orWord(index, incoming[index])
    }

    /** Serializes exactly the portable portion of Guava's `writeTo` format. */
    fun toByteArray(): ByteArray {
        val size = serializedSize()
        require(size <= Int.MAX_VALUE.toLong()) { "BloomFilter is too large for a ByteArray" }
        val output = ByteArray(size.toInt())
        output[0] = strategy.ordinal.toByte()
        output[1] = numHashFunctions.toByte()
        writeIntBigEndian(output, 2, bits.size)
        var offset = WIRE_HEADER_BYTES
        for (word in snapshot()) {
            writeLongBigEndian(output, offset, word)
            offset += Long.SIZE_BYTES
        }
        return output
    }

    override fun equals(other: Any?): Boolean =
        other === this ||
            (other is BloomFilter<*> &&
                numHashFunctions == other.numHashFunctions &&
                funnel == other.funnel &&
                strategy == other.strategy &&
                snapshot().contentEquals(other.snapshot()))

    override fun hashCode(): Int {
        var result = numHashFunctions
        result = 31 * result + funnel.hashCode()
        result = 31 * result + strategy.hashCode()
        result = 31 * result + snapshot().contentHashCode()
        return result
    }

    private fun hash(object_: T): Pair<Long, Long> {
        val sink = RecordingSink()
        funnel.funnel(object_, sink)
        val bytes = Hashing.murmur3_128().hashBytes(sink.toByteArray()).asBytes()
        return loadLittleEndianLong(bytes, 0) to loadLittleEndianLong(bytes, 8)
    }

    private fun setBit(bitIndex: Long): Boolean {
        val word = bits[(bitIndex ushr 6).toInt()]
        val mask = 1L shl (bitIndex and 63).toInt()
        while (true) {
            val old = word.load()
            if ((old and mask) != 0L) return false
            if (word.compareAndSet(old, old or mask)) return true
        }
    }

    private fun getBit(bitIndex: Long): Boolean {
        val word = bits[(bitIndex ushr 6).toInt()].load()
        val mask = 1L shl (bitIndex and 63).toInt()
        return (word and mask) != 0L
    }

    private fun orWord(index: Int, incoming: Long) {
        if (incoming == 0L) return
        val word = bits[index]
        while (true) {
            val old = word.load()
            val combined = old or incoming
            if (old == combined || word.compareAndSet(old, combined)) return
        }
    }

    private fun bitCount(): Long = bits.sumOf { it.load().countOneBits().toLong() }
    private fun snapshot(): LongArray = LongArray(bits.size) { bits[it].load() }

    private enum class Strategy {
        MURMUR128_MITZ_32 {
            override fun forEachIndex(
                hash1: Long,
                hash2: Long,
                count: Int,
                bitSize: Long,
                operation: (Long) -> Boolean,
            ) {
                val first = hash1.toInt()
                val second = (hash1 ushr 32).toInt()
                for (i in 1..count) {
                    var combined = first + i * second
                    if (combined < 0) combined = combined.inv()
                    if (!operation(combined.toLong() % bitSize)) return
                }
            }
        },
        MURMUR128_MITZ_64 {
            override fun forEachIndex(
                hash1: Long,
                hash2: Long,
                count: Int,
                bitSize: Long,
                operation: (Long) -> Boolean,
            ) {
                var combined = hash1
                repeat(count) {
                    if (!operation((combined and Long.MAX_VALUE) % bitSize)) return
                    combined += hash2
                }
            }
        };

        abstract fun forEachIndex(
            hash1: Long,
            hash2: Long,
            count: Int,
            bitSize: Long,
            operation: (Long) -> Boolean,
        )
    }

    private class RecordingSink : PrimitiveSink {
        private val data = ArrayList<Byte>()
        override fun putByte(b: Byte): PrimitiveSink = apply { data.add(b) }
        override fun putBytes(bytes: ByteArray): PrimitiveSink = apply { bytes.forEach(data::add) }
        override fun putInt(i: Int): PrimitiveSink = apply {
            repeat(Int.SIZE_BYTES) { shift -> putByte((i ushr (shift * 8)).toByte()) }
        }
        override fun putLong(l: Long): PrimitiveSink = apply {
            repeat(Long.SIZE_BYTES) { shift -> putByte((l ushr (shift * 8)).toByte()) }
        }
        override fun putUnencodedChars(chars: CharSequence): PrimitiveSink = apply {
            for (char in chars) {
                putByte(char.code.toByte())
                putByte((char.code ushr 8).toByte())
            }
        }
        fun toByteArray(): ByteArray = data.toByteArray()
    }

    companion object {
        private const val DEFAULT_FPP = 0.03
        private const val WIRE_HEADER_BYTES = 6
        private val LOG_TWO = ln(2.0)
        private val SQUARED_LOG_TWO = LOG_TWO * LOG_TWO

        fun <T> create(
            funnel: Funnel<in T>,
            expectedInsertions: Long,
            fpp: Double = DEFAULT_FPP,
        ): BloomFilter<T> {
            Preconditions.checkNotNull(funnel)
            require(expectedInsertions >= 0) { "Expected insertions ($expectedInsertions) must be >= 0" }
            require(fpp > 0.0) { "False positive probability ($fpp) must be > 0.0" }
            require(fpp < 1.0) { "False positive probability ($fpp) must be < 1.0" }

            val insertions = max(expectedInsertions, 1L)
            val requestedBits = optimalNumOfBits(insertions, fpp)
            val wordCount = ceil(requestedBits.toDouble() / Long.SIZE_BITS).toLong().coerceAtLeast(1L)
            require(wordCount <= Int.MAX_VALUE.toLong()) { "Could not create BloomFilter of $requestedBits bits" }
            val hashes = optimalNumOfHashFunctions(fpp)
            require(hashes <= 255) { "numHashFunctions ($hashes) must be <= 255" }
            return BloomFilter(LongArray(wordCount.toInt()), hashes, funnel, Strategy.MURMUR128_MITZ_64)
        }

        fun <T> create(
            funnel: Funnel<in T>,
            expectedInsertions: Int,
            fpp: Double = DEFAULT_FPP,
        ): BloomFilter<T> = create(funnel, expectedInsertions.toLong(), fpp)

        /** Reads Guava's strategy/hash-count/word-array wire format. */
        fun <T> readFrom(bytes: ByteArray, funnel: Funnel<in T>): BloomFilter<T> {
            Preconditions.checkNotNull(funnel)
            require(bytes.size >= WIRE_HEADER_BYTES) { "BloomFilter data is shorter than its header" }
            val strategyOrdinal = bytes[0].toInt()
            val strategy = Strategy.entries.getOrNull(strategyOrdinal)
                ?: throw IllegalArgumentException("Unknown BloomFilter strategy ordinal: $strategyOrdinal")
            val hashes = bytes[1].toInt() and 0xff
            require(hashes in 1..255) { "numHashFunctions ($hashes) must be between 1 and 255" }
            val wordCount = readIntBigEndian(bytes, 2)
            require(wordCount > 0) { "data length is zero" }
            val expectedSize = WIRE_HEADER_BYTES.toLong() + wordCount.toLong() * Long.SIZE_BYTES
            require(expectedSize == bytes.size.toLong()) {
                "BloomFilter data length ${bytes.size} does not match word count $wordCount"
            }
            val words = LongArray(wordCount)
            var offset = WIRE_HEADER_BYTES
            for (index in words.indices) {
                words[index] = readLongBigEndian(bytes, offset)
                offset += Long.SIZE_BYTES
            }
            return BloomFilter(words, hashes, funnel, strategy)
        }

        private fun optimalNumOfBits(insertions: Long, fpp: Double): Long {
            val probability = if (fpp == 0.0) Double.MIN_VALUE else fpp
            return (-insertions.toDouble() * ln(probability) / SQUARED_LOG_TWO).toLong()
        }

        private fun optimalNumOfHashFunctions(fpp: Double): Int =
            max(1, floor(-ln(fpp) / LOG_TWO + 0.5).toInt())

        private fun loadLittleEndianLong(bytes: ByteArray, offset: Int): Long {
            var value = 0L
            repeat(Long.SIZE_BYTES) { index ->
                value = value or ((bytes[offset + index].toLong() and 0xffL) shl (index * 8))
            }
            return value
        }

        private fun writeIntBigEndian(output: ByteArray, offset: Int, value: Int) {
            repeat(Int.SIZE_BYTES) { index -> output[offset + index] = (value ushr ((3 - index) * 8)).toByte() }
        }

        private fun readIntBigEndian(input: ByteArray, offset: Int): Int {
            var value = 0
            repeat(Int.SIZE_BYTES) { index -> value = (value shl 8) or (input[offset + index].toInt() and 0xff) }
            return value
        }

        private fun writeLongBigEndian(output: ByteArray, offset: Int, value: Long) {
            repeat(Long.SIZE_BYTES) { index -> output[offset + index] = (value ushr ((7 - index) * 8)).toByte() }
        }

        private fun readLongBigEndian(input: ByteArray, offset: Int): Long {
            var value = 0L
            repeat(Long.SIZE_BYTES) { index -> value = (value shl 8) or (input[offset + index].toLong() and 0xffL) }
            return value
        }
    }
}
