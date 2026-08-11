# Kotlin-first GuavaKt

**Goal:** Keep the Guava capabilities that fill real Kotlin Multiplatform gaps, and make them feel
native to Kotlin. This is a focused library, not a full Java API port.

## Rule

| If Kotlin already has it… | GuavaKt does… |
|---------------------------|---------------|
| Read-only `List` / `Set` / `Map`, `listOf` / `setOf` / `mapOf` | **Do not** invent Guava immutability. Prefer stdlib. Keep `Immutable*` only as thin Guava-name shims. |
| `ArrayList` / `HashMap` / `LinkedHashMap` / `HashSet` | `Lists` / `Maps` / `Sets` factories are **one-line aliases** to Kotlin/Java collections. Prefer `mutableListOf()`, `hashMapOf()`, etc. in new code. |
| `map` / `filter` / `flatMap` / `any` / `all` on collections | Prefer Kotlin stdlib. Guava `Iterables` / `Collections2` / `FluentIterable` stay for Guava-shaped call sites, implemented via stdlib. |
| Nullable types `T?` | Prefer over Guava `Optional` in new Kotlin. `Optional` remains for Guava fidelity. |
| `==` / `hashCode` | Prefer over `Objects.equal` / `Objects.hashCode` in new code. |
| Coroutines / structured concurrency | Prefer for async. GuavaKt offers only coroutine-native rate limiting and guarded coordination—not a futures/executor compatibility layer. |
| Guarded shared state | Prefer `CoroutineMonitor.withLockWhen` when a `Mutex` plus predicate would otherwise be repeated at each call site. |
| File storage | Pass an Okio `FileSystem` and `Path` on every target. There is no string-path or `java.nio.file.Path` façade. |
| Java `Method`, `Constructor`, `Proxy`, or `TypeVariable` | Keep them out of GuavaKt. Common code should use explicit dependencies, `KClass`, or reified APIs where appropriate. |

## What we *do* port (Kotlin has no full equivalent)

These are GuavaKt’s real product and fidelity priorities, implemented in portable Kotlin (or `expect`/`actual` only when required). Coverage is substantial but still alpha; consult the [compatibility matrix](compatibility.md) before assuming an edge-case contract.

- **Multimap / Multiset / Table / BiMap** (incl. live views, LinkedListMultimap entry order); use a nullable value type such as `ArrayTable<R, C, V?>` when fixed-grid empty cells are meaningful
- **ClassToInstanceMap** when a runtime-typed heterogeneous map is genuinely needed (`KClass`-based in common code)
- **Range / RangeSet / RangeMap / ContiguousSet**
- **Graph / ValueGraph / Network**
- **Traverser** for graph or tree traversal; deprecated collect `TreeTraverser` remains only for Guava-shaped migration
- **Cache / CacheBuilder / LoadingCache** (weak/soft on JVM)
- **Hashing / BloomFilter / Funnels** (pure Kotlin digests)
- **Preconditions** (still useful), **Joiner / Splitter / CharMatcher / Escapers**
- **InternetDomainName / PSL**, **HostAndPort**, net helpers
- **Stopwatch / Ticker**, **Suppliers** (memoize, expiration)
- **CoroutineRateLimiter** and **CoroutineMonitor** when a coroutine-specific coordination primitive is genuinely useful
- **Unsigned** primitives, **IntMath** / stats where Guava goes beyond stdlib

## Implementation style

1. **Default:** `commonMain` pure Kotlin using `kotlin.collections`.
2. **Guava names** when the *concept* is Guava-specific (`ArrayListMultimap`, `TreeRangeSet`).
3. **Kotlin names / stdlib** when the *concept* is “just a list/map/set/immutable view.”
4. **expect/actual** only for GC refs, filesystem, and real timers — never for “because Guava used Java.”

## Consumer guidance

```kotlin
// Prefer
val xs = listOf(1, 2, 3)
val m = mutableMapOf<String, Int>()
val y: String? = maybeName

// Guava-shaped when you need Guava concepts
val mm = ArrayListMultimap.create<String, Int>()
mm.get("k").add(1)
val cache = CacheBuilder.newBuilder<String, User>().maximumSize(100).build()

// Kotlin-first loading: the supplied scope owns shared per-key work
val users = CacheBuilder.newBuilder<String, User>()
    .maximumSize(100)
    .buildCoroutine(applicationScope) { id -> api.fetchUser(id) }
val user = users.get("42")

// Suspend rather than occupying a thread while waiting for rate-limit capacity
val limiter = CoroutineRateLimiter.create(20.0)
limiter.acquire()
api.send(request)

// Coordinate guarded state without blocking or polling on any KMP target
val monitor = CoroutineMonitor()
var pending = 0
val hasPending = monitor.newGuard { pending > 0 }
monitor.withLock { pending++ }
monitor.withLockWhen(hasPending) { pending-- }

// Make the storage boundary explicit and portable
Files.write(fileSystem, path, payload)
val bytes = Files.readAllBytes(fileSystem, path)
```

## Non-goals

- Bit-identical Guava heap layouts for `ImmutableList`
- Replacing Kotlin stdlib or kotlinx.coroutines
- Drop-in binary compatibility with `com.google.guava:guava`
- Java reflection/proxies, EventBus, Java executors/futures/services, or blocking queues


## Internal code rule

Inside GuavaKt **implementations** (non-shim bodies):

- Use `listOf` / `emptyList` / `toList()`, `setOf` / `emptySet` / `toSet()`, `mapOf` / `emptyMap` / `toMap()`.
- Do **not** call `ImmutableList.of` / `ImmutableSet.copyOf` / `ImmutableMap.copyOf` except:
  - Inside the `Immutable*` types themselves (shim factories), or
  - At a **public Guava-named API** that must return `Immutable*` for Guava-shaped signatures.

Example: `ImmutableListMultimap.Builder` snapshots with `vs.toList()`, not `ImmutableList.copyOf(vs)`.
