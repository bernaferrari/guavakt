package dev.guavakt.hash

interface HashFunction {
    fun bits(): Int
    fun hashBytes(input: ByteArray): HashCode = hashBytes(input, 0, input.size)
    fun hashBytes(input: ByteArray, off: Int, len: Int): HashCode
    fun hashInt(input: Int): HashCode
    fun hashLong(input: Long): HashCode
    fun hashUnencodedChars(input: CharSequence): HashCode
    fun hashString(input: CharSequence, charsetName: String = "UTF-8"): HashCode
    fun <T> hashObject(instance: T, funnel: Funnel<in T>): HashCode =
        newHasher().also { funnel.funnel(instance, it) }.hash()
    fun newHasher(): Hasher
}
