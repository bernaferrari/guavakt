package com.bernaferrari.guavakt.base

import com.bernaferrari.guavakt.annotations.GwtCompatible

@GwtCompatible
object MoreObjects {
    fun <T> firstNonNull(first: T?, second: T): T = first ?: second

    fun toStringHelper(self: Any): ToStringHelper = ToStringHelper(self::class.simpleName ?: "Object")

    fun toStringHelper(className: String): ToStringHelper = ToStringHelper(className)

    class ToStringHelper internal constructor(private val className: String) {
        private val holderHead = ValueHolder()
        private var holderTail = holderHead
        private var omitNullValues = false

        fun omitNullValues(): ToStringHelper = apply { omitNullValues = true }

        fun add(name: String, value: Any?): ToStringHelper = addHolder(name, value)
        fun add(name: String, value: Boolean): ToStringHelper = addHolder(name, value)
        fun add(name: String, value: Char): ToStringHelper = addHolder(name, value)
        fun add(name: String, value: Double): ToStringHelper = addHolder(name, value)
        fun add(name: String, value: Float): ToStringHelper = addHolder(name, value)
        fun add(name: String, value: Int): ToStringHelper = addHolder(name, value)
        fun add(name: String, value: Long): ToStringHelper = addHolder(name, value)

        fun addValue(value: Any?): ToStringHelper = addHolder(value)
        fun addValue(value: Boolean): ToStringHelper = addHolder(value)
        fun addValue(value: Char): ToStringHelper = addHolder(value)
        fun addValue(value: Double): ToStringHelper = addHolder(value)
        fun addValue(value: Float): ToStringHelper = addHolder(value)
        fun addValue(value: Int): ToStringHelper = addHolder(value)
        fun addValue(value: Long): ToStringHelper = addHolder(value)

        override fun toString(): String {
            val omitNullValuesSnapshot = omitNullValues
            var nextSeparator = ""
            val builder = StringBuilder(32).append(className).append('{')
            var valueHolder = holderHead.next
            while (valueHolder != null) {
                val value = valueHolder.value
                if (!omitNullValuesSnapshot || value != null) {
                    builder.append(nextSeparator)
                    nextSeparator = ", "
                    if (valueHolder.name != null) {
                        builder.append(valueHolder.name).append('=')
                    }
                    builder.append(value)
                }
                valueHolder = valueHolder.next
            }
            return builder.append('}').toString()
        }

        private fun addHolder(): ValueHolder {
            val valueHolder = ValueHolder()
            holderTail.next = valueHolder
            holderTail = valueHolder
            return valueHolder
        }

        private fun addHolder(value: Any?): ToStringHelper = apply {
            addHolder().value = value
        }

        private fun addHolder(name: String, value: Any?): ToStringHelper = apply {
            val valueHolder = addHolder()
            valueHolder.value = value
            valueHolder.name = Preconditions.checkNotNull(name)
        }

        private class ValueHolder {
            var name: String? = null
            var value: Any? = null
            var next: ValueHolder? = null
        }
    }
}
