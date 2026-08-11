# GuavaKt

**Kotlin Multiplatform primitives for the parts Kotlin’s standard library does not cover.**

![GuavaKt — Kotlin-first Multiplatform](assets/guavakt-social.png)

GuavaKt brings the portable, high-leverage ideas from Guava to Kotlin: rich collections, ranges,
graphs, hashing, networking helpers, exact math, and coroutine-aware coordination. It is designed
for `commonMain`, written in Kotlin, and uses coroutines and Okio where they are the natural fit.

It is independent of Google, uses `dev.guavakt.*` packages, and is not binary-compatible with
`com.google.guava:guava`. This is a Kotlin-first library, not a line-by-line port of Java-era
Guava.

## Install

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.guavakt:guavakt-collect:0.1.0")
}
```

Choose the module for the capability you need:

| Module | Includes |
|---|---|
| `guavakt-collect` | Multimap, Multiset, BiMap, Table, Range, RangeSet, RangeMap |
| `guavakt-graph` | Graph, ValueGraph, Network, Traverser |
| `guavakt-hash` | Hashing, Hasher, Funnel, BloomFilter |
| `guavakt-net` | IP parsing, HostAndPort, InternetDomainName |
| `guavakt-cache` | Bounded, expiring, coroutine-owned caches |
| `guavakt-concurrent` | Coroutine rate limiting and guarded coordination |
| `guavakt-math` | Primitive/statistical and arbitrary-precision math |
| `guavakt-io` | Okio-native sources, sinks, paths, and filesystem adapters |
| `guavakt` | The umbrella module |

## What it is for

| Need | Use |
|---|---|
| One key with many values, counted values, a two-way map, or a table | `Multimap`, `Multiset`, `BiMap`, `Table` |
| Intervals, disjoint ranges, range-to-value lookup, or huge discrete domains | `Range`, `RangeSet`, `RangeMap`, `ContiguousSet`, `DiscreteDomain.bigIntegers()` |
| Directed or undirected relationships | `Graph`, `ValueGraph`, `Network`, `Traverser` |
| Portable hashes or probabilistic membership | `Hashing`, `Hasher`, `Funnel`, `BloomFilter` |
| IP literals, ports, and registrable domains | `InetAddresses`, `HostAndPort`, `InternetDomainName` |
| A bounded, expiring cache with scoped asynchronous loading | `CacheBuilder.buildCoroutine(scope)` |
| A cancellable rate limit or guarded shared state | `CoroutineRateLimiter`, `CoroutineMonitor` |
| Exact values beyond primitive limits | `BigInteger`, `BigDecimal`, math helpers |

## A taste

```kotlin
import dev.guavakt.collect.ArrayListMultimap
import dev.guavakt.collect.Range
import dev.guavakt.collect.TreeRangeSet

val tags = ArrayListMultimap.create<String, String>()
tags.put("kotlin", "multiplatform")
tags.put("kotlin", "coroutines")

val maintenance = TreeRangeSet.create<Int>()
maintenance.add(Range.closedOpen(100, 200))
maintenance.contains(150) // true
maintenance.contains(200) // false
```

```kotlin
import dev.guavakt.cache.CacheBuilder
import kotlin.time.Duration.Companion.minutes

val users = CacheBuilder.newBuilder<UserId, User>()
    .maximumSize(1_000)
    .expireAfterAccess(30.minutes)
    .refreshAfterWrite(5.minutes)
    .buildCoroutine(applicationScope) { id -> api.fetchUser(id) }

val user = users.get(id) // suspends; same-key misses share one load
```

The supplied scope owns cache loads and refreshes. Cancelling one caller stops only its wait;
cancelling the owner scope stops the shared work.

## Kotlin first

Use Kotlin and focused libraries when they already provide the right abstraction:

- Kotlin `List`, `Set`, `Map`, sequences, nullable types, `require`, and `check`
- `kotlinx.coroutines`, `Flow`, `Channel`, `Mutex`, and structured concurrency
- Okio `FileSystem`, `Path`, `Source`, `Sink`, and `ByteString`
- Caffeine on the JVM when maximum local-cache throughput matters

GuavaKt deliberately does not include Java reflection or proxies, `ListenableFuture`, executors,
services, EventBus, blocking queues, or a `java.io`/`java.nio.file` facade.

## Documentation

- [Compatibility matrix](docs/compatibility.md) — supported concepts and intentional differences
- [Kotlin-first guide](docs/kotlin-first.md) — choosing Kotlin, coroutines, Okio, or GuavaKt
- [Architecture](docs/architecture.md) — modules and targets
- [Stability policy](docs/stability.md), [changelog](CHANGELOG.md), and [security policy](SECURITY.md)
- [Documentation index](docs/README.md) — benchmarks, fuzzing, public-suffix refreshes, and releases
- [Contributor guide](AGENTS.md) — project conventions and verification commands
