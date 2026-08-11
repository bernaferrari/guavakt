# GuavaKt roadmap

## Direction

GuavaKt is a focused Kotlin Multiplatform library for concepts Kotlin does not already solve well.
The target is not “all Guava classes.” A capability is included only when it is useful, portable,
and can be implemented honestly with Kotlin, coroutines, and Okio.

For every candidate, choose one of these outcomes:

1. **Port** — a differentiated, portable concept such as Multimap, Range, Graph, cache, hashing,
   or public-suffix logic.
2. **Adapt** — the capability is worthwhile but Kotlin needs a better shape, such as coroutine
   cache loading, `CoroutineRateLimiter`, `CoroutineMonitor`, `KClass` type keys, or Okio I/O.
3. **Guide elsewhere** — Kotlin stdlib, `kotlinx.coroutines`, Okio, or a focused library is the
   clearer solution.
4. **Exclude** — a Java-runtime mechanism has no good KMP meaning or would become a dishonest shim.

The last category includes reflection/proxies, EventBus, `ListenableFuture`, executors, services,
blocking queues/monitors, and Java filesystem bridges. Their removal is a maintained product
decision, not backlog.

## What developers ask for

| Signal | Product response |
|---|---|
| Kotlin lacks a standard Multimap | Keep Multimap/Multiset/BiMap/Table as the flagship, with correct live views, ordering, equality, and mutation bookkeeping |
| JVM Guava cannot serve JS, Wasm, and Native | Prefer common algorithms and explicit platform tiers over JVM adapters |
| KMP applications need bounded expiring caching | Prioritize scope-owned loaders, cancellation, invalidation races, predictable eviction, and useful statistics |
| Java I/O leaks into shared code | Use injected Okio `FileSystem`, `Path`, `Source`, `Sink`, and `ByteString` |
| Coroutine users need back-pressure tools | Provide suspension-first rate limiting and guarded coordination; do not recreate Java thread APIs |
| Range/graph/hash/math bugs are costly | Favor direct oracle traces, fuzzing, and reproducible performance evidence over more class names |

The evidence behind this direction is recorded in the README and existing differential suites. Once
the project has users, reports from real applications override generic popularity signals.

## Priorities

### P0 — make the differentiated core dependable

1. Keep Multimap, Multiset, BiMap, Table, and range live-view behavior deeply tested.
2. Improve coroutine-cache behavior from real workload evidence: cancellation, stampede control,
   refresh/invalidation races, and documented performance trade-offs.
3. Keep Okio I/O portable and explicit; exercise Node and Native filesystem behavior where CI can
   run it.
4. Maintain coroutine primitives with virtual-time and cancellation tests. Expand rate-limiter and
   monitor coverage only when it protects a real KMP use case.
5. Publish clear coordinates, generated API docs, samples, release policy, and compatibility
   guarantees before calling the library stable.

### P1 — preserve the hard algorithms

1. Continue seeded Range/RangeSet/RangeMap fuzzing, including arbitrary-precision domains.
2. Continue seeded Graph/ValueGraph/Network mutation traces and ordering tests.
3. Refresh public-suffix data deliberately, with a semantic diff and reviewed generated trie.
4. Keep hash/Bloom-filter wire formats and streaming behavior differential-tested; use benchmarks to
   investigate regressions rather than setting machine-specific throughput gates.
5. Extend arbitrary-precision math only where it benefits Guava math APIs, range domains, or real
   KMP callers. Do not promise a complete JDK `BigInteger`/`BigDecimal` replacement.

### P2 — release discipline

1. Keep the pinned Guava/JDK oracle current enough to expose meaningful behavioral changes.
2. Record intentional API removals and compatibility boundaries in the matrix rather than leaving
   placeholders.
3. Maintain API baselines, Dokka, reproducible fuzzing, and publication validation.
4. Establish versioning, deprecation, support-window, and security-response policy for a stable
   release.

## What success looks like

The project is ready to leave alpha when the following are true:

1. Core capabilities have a stable, documented API and an explicit support policy.
2. The JVM oracle, common tests, JS, Wasm, and supported Native CI targets are green.
3. The structural and immutable audits are green with no compatibility shells that fake unavailable
   platform behavior.
4. Maven coordinates, SCM metadata, signing, reproducible publications, and API documentation are
   verified.
5. The team can explain which guarantees are stable and which target-specific trade-offs remain.

## Definition of done for a capability

1. Its Kotlin API and intentional differences are documented in the compatibility matrix.
2. Behavior lives in `commonMain` unless a real platform facility requires otherwise.
3. Useful Guava/JDK semantics have typed differential tests, including failures and mutable views.
4. Kotlin-specific semantics have tests for nullability, suspension, cancellation, ownership, and
   `Duration` where relevant.
5. The relevant cross-platform tests, API checks, and structural audits pass.
6. Performance-sensitive work has a reproducible benchmark or trace and no known pathological
   algorithmic regression.
