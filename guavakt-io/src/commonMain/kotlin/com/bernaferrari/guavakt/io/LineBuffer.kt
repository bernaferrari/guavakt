package com.bernaferrari.guavakt.io

/** Guava LineBuffer — accumulates chars and fires complete lines. */
internal abstract class LineBuffer {
    private val line = StringBuilder()
    private var sawReturn = false

    fun add(cbuf: CharArray, off: Int, len: Int) {
        var pos = off
        if (sawReturn && len > 0) {
            if (finishLine(cbuf[pos] == '\n')) pos++
        }
        val end = off + len
        var start = pos
        while (pos < end) {
            when (cbuf[pos]) {
                '\r' -> {
                    line.appendRange(cbuf, start, pos)
                    sawReturn = true
                    if (pos + 1 < end) {
                        if (finishLine(cbuf[pos + 1] == '\n')) pos++
                    }
                    start = pos + 1
                }
                '\n' -> {
                    line.appendRange(cbuf, start, pos)
                    finishLine(true)
                    start = pos + 1
                }
            }
            pos++
        }
        line.appendRange(cbuf, start, end)
    }

    fun finish() {
        if (sawReturn || line.isNotEmpty()) finishLine(false)
    }

    private fun finishLine(sawNewline: Boolean): Boolean {
        val separator = when {
            sawReturn -> if (sawNewline) "\r\n" else "\r"
            sawNewline -> "\n"
            else -> ""
        }
        handleLine(line.toString(), separator)
        line.clear()
        sawReturn = false
        return sawNewline
    }

    protected abstract fun handleLine(line: String, end: String)
}
