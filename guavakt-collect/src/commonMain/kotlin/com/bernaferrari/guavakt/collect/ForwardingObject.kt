package com.bernaferrari.guavakt.collect

/**
 * Base for decorator-style forwarding types.
 *
 * [toString] is forwarded, while equality and hashing deliberately retain identity semantics.
 * Forwarding those methods here would make equality asymmetric because this base implements no
 * value-defining interface.
 */
abstract class ForwardingObject {
    protected abstract fun delegate(): Any

    override fun toString(): String = delegate().toString()
}
