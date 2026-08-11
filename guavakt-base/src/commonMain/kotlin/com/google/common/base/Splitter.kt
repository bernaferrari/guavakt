package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible

@GwtCompatible
class Splitter private constructor(
    private val strategy: Strategy,
    private val omitEmptyStrings: Boolean = false,
    private val trimmer: (String) -> String = { it },
    private val limit: Int = Int.MAX_VALUE,
) {
    fun omitEmptyStrings(): Splitter = Splitter(strategy, true, trimmer, limit)

    fun trimResults(): Splitter = Splitter(strategy, omitEmptyStrings, { it.trim() }, limit)

    fun limit(limit: Int): Splitter {
        Preconditions.checkArgument(limit > 0, "must be greater than zero: %s", limit)
        return Splitter(strategy, omitEmptyStrings, trimmer, limit)
    }

    fun withKeyValueSeparator(separator: String): MapSplitter = MapSplitter(this, on(separator))
    fun withKeyValueSeparator(separator: Char): MapSplitter = withKeyValueSeparator(separator.toString())
    fun withKeyValueSeparator(keyValueSplitter: Splitter): MapSplitter = MapSplitter(this, keyValueSplitter)

    fun split(sequence: CharSequence): Iterable<String> = splitToList(sequence)

    fun splitToList(sequence: CharSequence): List<String> {
        Preconditions.checkNotNull(sequence)
        val segments = strategy.split(sequence)
        val result = ArrayList<String>(segments.size)
        var remaining = limit
        for (segment in segments) {
            val trimmed = trimmer(sequence.subSequence(segment.start, segment.end).toString())
            if (omitEmptyStrings && trimmed.isEmpty()) continue
            if (remaining == 1) {
                result.add(trimmer(sequence.subSequence(segment.start, sequence.length).toString()))
                return result
            }
            result.add(trimmed)
            remaining--
        }
        return result
    }

    private fun interface Strategy {
        fun split(sequence: CharSequence): List<Segment>
    }

    private data class Segment(val start: Int, val end: Int)

    companion object {
        fun on(separator: Char): Splitter = on(separator.toString())

        fun on(separatorMatcher: CharMatcher): Splitter {
            Preconditions.checkNotNull(separatorMatcher)
            return Splitter(
                Strategy { sequence ->
                    val parts = ArrayList<Segment>()
                    var start = 0
                    while (true) {
                        val separatorIndex = separatorMatcher.indexIn(sequence, start)
                        if (separatorIndex < 0) break
                        parts.add(Segment(start, separatorIndex))
                        start = separatorIndex + 1
                    }
                    parts.add(Segment(start, sequence.length))
                    parts
                },
            )
        }

        fun on(separator: String): Splitter {
            Preconditions.checkNotNull(separator)
            Preconditions.checkArgument(separator.isNotEmpty(), "The separator may not be the empty string.")
            return Splitter(
                Strategy { sequence ->
                    val parts = ArrayList<Segment>()
                    var start = 0
                    val text = sequence.toString()
                    while (true) {
                        val idx = text.indexOf(separator, start)
                        if (idx < 0) break
                        parts.add(Segment(start, idx))
                        start = idx + separator.length
                    }
                    parts.add(Segment(start, text.length))
                    parts
                },
            )
        }

        fun onPattern(separatorPattern: String): Splitter {
            Preconditions.checkNotNull(separatorPattern)
            val regex = Regex(separatorPattern)
            Preconditions.checkArgument(
                !regex.matches(""),
                "The pattern may not match the empty string: %s",
                separatorPattern,
            )
            return Splitter(
                Strategy { sequence ->
                    val text = sequence.toString()
                    val parts = ArrayList<Segment>()
                    var start = 0
                    var searchStart = 0
                    while (searchStart <= text.length) {
                        val match = regex.find(text, searchStart) ?: break
                        val end = match.range.last + 1
                        if (start == 0 && match.range.first == 0 && end == 0) {
                            searchStart = 1
                            continue
                        }
                        parts.add(Segment(start, match.range.first))
                        start = end
                        searchStart = if (end == match.range.first) match.range.first + 1 else end
                    }
                    parts.add(Segment(start, text.length))
                    parts
                },
            )
        }

        fun fixedLength(length: Int): Splitter {
            Preconditions.checkArgument(length > 0, "The length may not be less than 1")
            return Splitter(
                Strategy { sequence ->
                    val text = sequence.toString()
                    val parts = ArrayList<Segment>()
                    var start = 0
                    while (start < text.length) {
                        val end = minOf(text.length, start + length)
                        parts.add(Segment(start, end))
                        start = end
                    }
                    if (parts.isEmpty()) parts.add(Segment(0, 0))
                    parts
                },
            )
        }
    }
}

/** Guava MapSplitter — produces a Kotlin [Map]. */
class MapSplitter internal constructor(
    private val outer: Splitter,
    private val entrySplitter: Splitter,
) {
    fun split(sequence: CharSequence): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (entry in outer.splitToList(sequence)) {
            val parts = entrySplitter.limit(2).splitToList(entry)
            require(parts.size == 2) { "Chunk [$entry] is not a valid entry" }
            val key = parts[0]
            require(key !in map) { "Duplicate key [$key] found." }
            map[key] = parts[1]
        }
        return map
    }
}
