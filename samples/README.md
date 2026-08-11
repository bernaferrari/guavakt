# GuavaKt samples

These snippets describe the public `0.1.0` API.

## A live Multimap view

```kotlin
import dev.guavakt.collect.ArrayListMultimap

val byLanguage = ArrayListMultimap.create<String, String>()
val kotlinLibraries = byLanguage.get("kotlin")
kotlinLibraries += "guavakt"

check(byLanguage.containsEntry("kotlin", "guavakt"))
```

`get(key)` is a live view: mutating it changes the multimap and its key bookkeeping.

## Okio-first filesystem I/O

```kotlin
import dev.guavakt.io.ByteSource
import dev.guavakt.io.Files
import okio.FileSystem
import okio.Path

fun save(fileSystem: FileSystem, path: Path, bytes: ByteArray) {
    Files.asByteSink(fileSystem, path).write(bytes)
    check(Files.asByteSource(fileSystem, path).contentEquals(ByteSource.wrap(bytes)))
}
```

Accept the `FileSystem` and `Path` at your boundary. This works with a production filesystem where
available and with Okio's fake filesystem in common tests; it does not smuggle `java.io.File` into
shared code.

## Hash an Okio stream

```kotlin
import dev.guavakt.hash.Hashing
import dev.guavakt.hash.hashing
import okio.Buffer

val source = Buffer().writeUtf8("portable payload").hashing(Hashing.sha256())
val destination = Buffer()
while (source.read(destination, 8_192L) >= 0L) Unit
val digest = source.hash()
source.close()
```

`HashingSource` and `HashingSink` are the common-code replacements for Guava's JVM stream
wrappers. They delegate close to the wrapped Okio resource and never hash unread or unwritten data.

## Hash a domain object explicitly

```kotlin
import dev.guavakt.hash.BloomFilter
import dev.guavakt.hash.Funnel
import dev.guavakt.hash.Hashing

data class Event(val id: Long, val active: Boolean, val title: String)

val eventFunnel = Funnel<Event> { event, sink ->
    sink.putLong(event.id).putBoolean(event.active).putString(event.title)
}
val event = Event(42, true, "Kotlin Multiplatform")
val digest = Hashing.sha256().hashObject(event, eventFunnel)
val seen = BloomFilter.create(eventFunnel, expectedInsertions = 10_000)
seen.put(event)
check(seen.mightContain(event))
```

Funnels should use an explicit, stable field order and encoding. They are the portable alternative
to reflection-based object hashing, and the same funnel can be reused for `hashObject` and a
`BloomFilter`.

## A directed dependency graph

```kotlin
import dev.guavakt.graph.GraphBuilder
import dev.guavakt.graph.Graphs

val graph = GraphBuilder.directed<String>().build<String>()
graph.putEdge("app", "domain")
graph.putEdge("domain", "storage")

val consumers = Graphs.transpose(graph)
check(consumers.successors("storage") == setOf("domain"))
```

The transposed graph is a live read-only view. Later changes to `graph` are visible through `consumers`.

## A bounded loading cache

```kotlin
import dev.guavakt.cache.CacheBuilder
import dev.guavakt.cache.CacheLoader

val cache = CacheBuilder.newBuilder<String, Int>()
    .maximumSize(100)
    .recordStats()
    .build(object : CacheLoader<String, Int>() {
        override fun load(key: String): Int = key.length
    })

check(cache.get("kotlin") == 6)
```

On JVM, concurrent loads of the same missing key are coalesced. The current alpha implementation uses one cache lock, so unrelated loads may serialize.
