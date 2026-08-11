package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible

@GwtCompatible
class Joiner private constructor(
    private val separator: String,
    private val nullText: String? = null,
    private val skipNulls: Boolean = false,
) {
    fun skipNulls(): Joiner {
        Preconditions.checkState(nullText == null)
        return Joiner(separator, nullText = null, skipNulls = true)
    }

    fun useForNull(nullText: String): Joiner {
        Preconditions.checkNotNull(nullText)
        Preconditions.checkState(!skipNulls)
        return Joiner(separator, nullText = nullText, skipNulls = false)
    }

    fun appendTo(appendable: Appendable, parts: Iterator<*>): Appendable {
        Preconditions.checkNotNull(appendable)
        if (skipNulls) {
            var first = true
            while (parts.hasNext()) {
                val part = parts.next() ?: continue
                if (!first) appendable.append(separator)
                appendable.append(part.toString())
                first = false
            }
            return appendable
        }
        if (parts.hasNext()) {
            appendable.append(toString(parts.next()))
            while (parts.hasNext()) {
                appendable.append(separator)
                appendable.append(toString(parts.next()))
            }
        }
        return appendable
    }

    fun appendTo(builder: StringBuilder, parts: Iterator<*>): StringBuilder {
        appendTo(builder as Appendable, parts)
        return builder
    }

    fun join(parts: Iterable<*>): String = join(parts.iterator())

    fun join(parts: Iterator<*>): String = appendTo(StringBuilder(), parts).toString()

    fun join(parts: Array<out Any?>): String = join(parts.asList())

    fun join(first: Any?, second: Any?, vararg rest: Any?): String {
        val list = ArrayList<Any?>(2 + rest.size)
        list.add(first)
        list.add(second)
        rest.forEach { list.add(it) }
        return join(list)
    }

    fun withKeyValueSeparator(keyValueSeparator: String): MapJoiner =
        MapJoiner(this, Preconditions.checkNotNull(keyValueSeparator))

    private fun toString(part: Any?): String {
        if (part == null) {
            return Preconditions.checkNotNull(nullText) { "Joiner does not support null elements; use useForNull or skipNulls" }
        }
        return part.toString()
    }

    class MapJoiner internal constructor(
        private val joiner: Joiner,
        private val keyValueSeparator: String,
    ) {
        fun join(map: Map<*, *>): String = join(map.entries)

        fun join(entries: Iterable<Map.Entry<*, *>>): String {
            val sb = StringBuilder()
            val it = entries.iterator()
            if (it.hasNext()) {
                appendEntry(sb, it.next())
                while (it.hasNext()) {
                    sb.append(joiner.separator)
                    appendEntry(sb, it.next())
                }
            }
            return sb.toString()
        }

        private fun appendEntry(sb: StringBuilder, entry: Map.Entry<*, *>) {
            sb.append(entry.key.toString())
            sb.append(keyValueSeparator)
            sb.append(entry.value.toString())
        }
    }

    companion object {
        fun on(separator: String): Joiner = Joiner(Preconditions.checkNotNull(separator))
        fun on(separator: Char): Joiner = Joiner(separator.toString())
    }
}
