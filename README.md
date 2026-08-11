# GuavaKt

**The KMP primitives Kotlin does not have — not a kitchen-sink port of Guava.**

GuavaKt brings the genuinely useful, portable ideas from Guava to Kotlin Multiplatform: rich
collections, ranges, graphs, hashing, network parsing, exact math, and coroutine-aware
coordination. It is designed for `commonMain` and uses Kotlin, coroutines, and Okio where they are
the better tool.

![GuavaKt — Kotlin-first Multiplatform](assets/guavakt-social.png)

> **Early alpha.** GuavaKt is independent of Google and uses `dev.guavakt.*` packages. It is not
> binary-compatible with `com.google.guava:guava`, and it does not aim to reproduce every Java-era
> convenience API.

## The point

Use GuavaKt when a concept is missing from Kotlin's common standard library and you need one API
across JVM, JS, Wasm, and Native.

| Need | GuavaKt provides |
|---|---|
| One key with many values, counted values, a two-way map, or a table | `Multimap`, `Multiset`, `BiMap`, `Table` |
| Intervals, disjoint ranges, range-to-value lookup, or huge discrete domains | `Range`, `RangeSet`, `RangeMap`, `ContiguousSet`, `DiscreteDomain.bigIntegers()` |
| Directed/undirected relationships with nodes, edge values, or edge objects | `Graph`, `ValueGraph`, `Network`, `Traverser` |
| Portable hashes or probabilistic membership | `Hashing`, `Hasher`, `Funnel`, `BloomFilter` |
| IP literals, ports, and registrable-domain logic without JVM networking objects | `InetAddresses`, `HostAndPort`, `InternetDomainName` |
| A bounded, expiring cache whose loads belong to a coroutine scope | `CacheBuilder.buildCoroutine(scope)` |
| A cancellable rate limit or guarded shared state | `CoroutineRateLimiter`, `CoroutineMonitor` |
| Exact values beyond primitive limits | `BigInteger`, `BigDecimal`, Guava-shaped math helpers |

## Not the point

GuavaKt deliberately stays out of the way when Kotlin or a focused library already wins.

| Prefer | Instead of |
|---|---|
| Kotlin `List`, `Set`, `Map`, sequence operators, `T?`, `require`, and `check` | Collection factories, `Optional`, and ordinary base helpers |
| `kotlinx.coroutines`, `Flow`, `Channel`, `Mutex`, and structured concurrency | New `ListenableFuture`, executor, blocking monitor, or service code |
| Okio `FileSystem`, `Path`, `Source`, `Sink`, and `ByteString` | A fake portable `java.io` or `java.nio.file` layer |
| Caffeine on JVM when peak local-cache performance is the goal | Treating a Guava-shaped cache as the JVM performance leader |
| Explicit dependencies or typed `Flow`/`Channel` pipelines | New EventBus designs |

The last point is intentional: Guava itself recommends against new EventBus use in favour of more
explicit/reactive designs. GuavaKt's EventBus exists only as a migration-oriented module and is not
part of the recommended API for new code. See [Guava's guidance](https://github.com/google/guava/wiki/EventBusExplained).

## A taste

### A multimap, not `Map<K, MutableList<V>>` bookkeeping

```kotlin
import dev.guavakt.collect.ArrayListMultimap

val tags = ArrayListMultimap.create<String, String>()
tags.put("kotlin", "multiplatform")
tags.put("kotlin", "coroutines")

val kotlinTags = tags["kotlin"] // [multiplatform, coroutines]
```

### A range set, not a pile of boundary checks

```kotlin
import dev.guavakt.collect.Range
import dev.guavakt.collect.TreeRangeSet

val maintenance = TreeRangeSet.create<Int>()
maintenance.add(Range.closedOpen(100, 200))

maintenance.contains(150) // true
maintenance.contains(200) // false
```

### A cache with explicit coroutine ownership

```kotlin
import dev.guavakt.cache.CacheBuilder
import kotlin.time.Duration.Companion.minutes

val users = CacheBuilder.newBuilder<UserId, User>()
    .maximumSize(1_000)
    .expireAfterAccess(30.minutes)
    .refreshAfterWrite(5.minutes)
    .recordStats()
    .buildCoroutine(applicationScope) { id -> api.fetchUser(id) }

val user = users.get(id) // suspends; same-key misses share one load
```

The supplied scope owns loads and refreshes. Cancelling one caller stops only its wait; cancelling
the owner scope stops its shared work. That is the new-code API. The synchronous builder shape is
retained for migration, not as the preferred concurrency model.

## What to depend on

Use the smallest module that fits the capability. The umbrella module is convenient for exploring
the source tree, but production consumers should not need an all-or-nothing utility bundle.

| Module | Intended new-code use |
|---|---|
| `guavakt-collect` | Rich collections and ranges |
| `guavakt-graph` | Graphs, networks, traversal |
| `guavakt-hash` | Hash functions, funnels, Bloom filters |
| `guavakt-net` | IP, host/port, and public-suffix utilities |
| `guavakt-cache` | Coroutine-owned in-memory caching |
| `guavakt-concurrent` | Coroutine rate limiting and guarded coordination |
| `guavakt-math` | Primitive/statistical and arbitrary-precision math |
| `guavakt-io` | Okio-native source/sink and filesystem adapters |

`base`, `escape`, `primitives`, annotations, and the Guava-shaped future/reflection/EventBus
modules support the core or migration use cases. They are not the reason to adopt GuavaKt.

## Status and honesty

There are no published artifacts yet; evaluate the project from source until the first tagged
release documents supported coordinates. This is alpha software, not a drop-in Guava replacement.

High-value contracts are tested directly against pinned Guava 33.6 on the JVM, alongside JS, Wasm,
and Linux Native test suites. The project has seeded range/graph/math traces, coroutine
cancellation tests, Bloom-filter wire checks, and reproducible cache/graph/hash benchmarks.

The current JVM parity gate has two known `BigDecimal` scale/presentation discrepancies, so treat
arbitrary-precision decimal support as evaluation-only until that gate is green. Platform-specific
facilities—weak references, real blocking, classpath scanning, proxies, and system filesystems—are
always called out rather than silently approximated.

## Learn more

- [Compatibility matrix](docs/compatibility.md) — evidence level, intentional differences, and platform limits
- [Kotlin-first guide](docs/kotlin-first.md) — when Kotlin, coroutines, or Okio are the better API
- [Architecture](docs/architecture.md) — module graph and target policy
- [Roadmap](docs/roadmap.md) — priorities and definition of done
- [Contributor guide](AGENTS.md) — verification gates and project conventions

The full documentation index, including benchmarks, fuzzing, public-suffix refreshes, and release
discipline, is in [docs](docs/README.md).
