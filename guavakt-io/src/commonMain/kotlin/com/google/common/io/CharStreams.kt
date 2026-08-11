package dev.guavakt.io

/**
 * Guava CharStreams — character utilities.
 *
 * Common: [CharSequence] / [Appendable] APIs.
 * JVM: extension overloads on [CharStreams] for `java.io.Reader` / `Writer`
 * (see `CharStreamsJvm.kt` in jvmMain).
 */
object CharStreams {
    fun toString(chars: CharSequence): String = chars.toString()

    fun readLines(chars: CharSequence): List<String> =
        readLines(chars, object : LineProcessor<MutableList<String>> {
            private val lines = mutableListOf<String>()
            override fun processLine(line: String): Boolean {
                lines += line
                return true
            }
            override fun getResult(): MutableList<String> = lines
        })

    /** Processes CR, LF, and CRLF logical lines without materializing an intermediate split list. */
    fun <T> readLines(chars: CharSequence, processor: LineProcessor<T>): T {
        val reader = LineReader(CharReaderLike(chars.toString()))
        while (true) {
            val line = reader.readLine() ?: return processor.getResult()
            if (!processor.processLine(line)) return processor.getResult()
        }
    }

    fun copy(from: CharSequence, to: Appendable): Long {
        val s = from.toString()
        to.append(s)
        return s.length.toLong()
    }

    fun copy(from: CharSequence, to: StringBuilder): Long {
        val s = from.toString()
        to.append(s)
        return s.length.toLong()
    }

    fun skipFully(chars: CharSequence, n: Long): CharSequence {
        require(n >= 0)
        val s = chars.toString()
        if (n >= s.length) return ""
        return s.substring(n.toInt())
    }

    fun nullWriter(): Appendable = object : Appendable {
        override fun append(value: Char): Appendable = this
        override fun append(value: CharSequence?): Appendable = this
        override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable = this
    }

    fun asWriter(target: Appendable): Appendable = target
}
