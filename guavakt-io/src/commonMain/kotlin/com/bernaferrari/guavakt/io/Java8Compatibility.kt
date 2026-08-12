package com.bernaferrari.guavakt.io

/** Guava Java8Compatibility — transferTo / readAllBytes style helpers on KMP streams. */
internal object Java8Compatibility {
    fun transferTo(input: ByteArrayInputLike, output: ByteArrayOutputLike): Long {
        var total = 0L
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            output.write(buf, 0, n)
            total += n
        }
        return total
    }
}
