package dev.guavakt.graph

/** Guava StandardValueGraph — mutable value graph implementation. */
class StandardValueGraph<N, V>(
    directed: Boolean,
    allowsSelfLoops: Boolean,
) : StandardMutableValueGraph<N, V>(directed, allowsSelfLoops)
