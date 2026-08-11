package dev.guavakt.collect

/**
 * Named synchronized-map stand-in for KMP.
 *
 * On multiplatform we cannot offer JVM monitor semantics for all targets; this wrapper
 * still isolates the map behind a dedicated type so call sites match Guava's
 * [Maps.synchronizedMap] shape. Prefer real `Collections.synchronizedMap` on JVM-only code
 * if you need monitor-based concurrency.
 */
internal class SynchronizedMap<K, V>(
    private val delegate: MutableMap<K, V>,
) : MutableMap<K, V> by delegate
