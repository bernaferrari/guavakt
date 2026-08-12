package com.bernaferrari.guavakt.graph

/** Guava GraphsBridgeMethods — package bridge helpers for Graphs utility. */
internal object GraphsBridgeMethods {
    fun <N> nodeInvalidatableSet(nodes: Set<N>, graph: Graph<N>): Set<N> = nodes
}
