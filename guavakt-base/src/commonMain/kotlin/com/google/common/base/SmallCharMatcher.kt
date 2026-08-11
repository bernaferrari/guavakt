package dev.guavakt.base

/**
 * Guava SmallCharMatcher — bit-set char matcher for small character sets (C1 control + Latin-1 subset).
 */
internal class SmallCharMatcher private constructor(
    private val table: LongArray, // 65536 bits / 64 = 1024 longs for full BMP would be large; use open addressing set
    private val filter: Long,
    private val chars: CharArray,
) : CharMatcher() {
    override fun matches(c: Char): Boolean {
        val index = (c.code * -0x61c88647) // smear
        // Linear probe in chars array
        var i = index and (chars.size - 1)
        val start = i
        while (true) {
            val ch = chars[i]
            if (ch.code == 0 && chars.indexOf(c) < 0 && c.code != 0) {
                // empty slot — not present (except NUL handled carefully)
                if (c.code == 0) {
                    // check if 0 is stored
                    return chars.any { it.code == 0 && filter != 0L }
                }
                return false
            }
            if (ch == c) return true
            i = (i + 1) and (chars.size - 1)
            if (i == start) return false
        }
    }

    companion object {
        const val MAX_SIZE = 1023

        fun from(charsSet: Set<Char>, description: String): CharMatcher {
            if (charsSet.isEmpty()) return none()
            val tableSize = (charsSet.size * 2).coerceAtLeast(2).takeHighestOneBit().let {
                if (it < charsSet.size * 2) it * 2 else it
            }.coerceAtLeast(2)
            val arr = CharArray(tableSize)
            var filter = 0L
            for (c in charsSet) {
                filter = filter or (1L shl c.code)
                var i = (c.code * -0x61c88647) and (tableSize - 1)
                while (arr[i].code != 0 || (c.code == 0 && arr.contains(0.toChar()) && i != arr.indexOf(0.toChar()))) {
                    if (arr[i] == c) break
                    if (arr[i].code == 0) { arr[i] = c; break }
                    i = (i + 1) and (tableSize - 1)
                }
                if (arr[i].code == 0) arr[i] = c
            }
            return SmallCharMatcher(LongArray(0), filter, arr)
        }
    }
}
