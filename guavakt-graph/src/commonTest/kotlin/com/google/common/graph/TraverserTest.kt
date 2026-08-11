package dev.guavakt.graph

import kotlin.test.Test
import kotlin.test.assertEquals

class TraverserTest {
    @Test
    fun breadthFirst_order() {
        val g: MutableGraph<String> = GraphBuilder.directed<String>().build()
        g.putEdge("a", "b")
        g.putEdge("a", "c")
        g.putEdge("b", "d")
        assertEquals(listOf("a", "b", "c", "d"), Traverser.forGraph(g).breadthFirst("a").toList())
    }
}
