# GuavaKt compatibility scope

GuavaKt brings selected Guava-shaped capabilities to Kotlin Multiplatform. It is not source or
binary compatible with Guava, and it is deliberately not a complete Guava rewrite.

## Evidence levels

- **Differential:** representative values and failures run directly against the pinned Guava/JDK
  oracle on the JVM.
- **Contract-tested:** behavior is tested from the public contract without a JVM oracle for every
  case.
- **Kotlin-shaped:** the capability is preserved, but signatures deliberately fit Kotlin and KMP.
- **Platform-limited:** a runtime facility is available only where the target can supply it
  honestly.

## Current surface

| Area | Level | Supported focus | Deliberate boundary |
|---|---|---|---|
| `base` | Differential / contract-tested | Preconditions, `Optional`, strings, `Splitter`, `Joiner`, `CharMatcher`, converters, suppliers, clocks, and portable regex behavior | Kotlin nullability and stdlib collections are preferred; no Java enum/reflection helpers |
| `collect` | Differential / contract-tested | Multimaps, multisets, `BiMap`, `Table`, immutable snapshots, traversal helpers, `KClass`-based class-to-instance maps, live views, comparator-aware sorted types | Ordinary collections remain stdlib territory; no fake concurrent/synchronized maps or Java blocking queues |
| ranges | Differential / contract-tested | `Range`, `RangeSet`, `RangeMap`, `ContiguousSet`, including arbitrary-precision `DiscreteDomain.bigIntegers()` | Kotlin sets/maps replace Java `Navigable*` interfaces |
| `graph` | Differential / contract-tested | `Graph`, `ValueGraph`, `Network`, traversal, ordering, mutation, transpose/copy utilities | No Java binary compatibility or private implementation parity |
| `hash` | Differential / contract-tested | Streaming hashes, HMAC, checksums, Bloom filters, funnels, Okio hashing sources/sinks | Algorithms document their allocation/performance trade-offs per target |
| `cache` | Contract-tested / Kotlin-shaped | Bounded/expiring cache behavior, stats, invalidation, and scope-owned coroutine loading with per-key coalescing | Prefer `buildCoroutine(scope)` for new code; weak keys are not fully GC/identity equivalent off JVM |
| `concurrent` | Contract-tested / Kotlin-shaped | Suspending `CoroutineRateLimiter` with burst/warm-up modes, cancellation-safe reservations, timeout attempts, and stats; `CoroutineMonitor` guarded `Mutex` coordination with state-change wakeups | No `ListenableFuture`, executor, service, Java blocking monitor, atomic-map, or time-limiter API. Use coroutines, `Flow`, `Channel`, `Mutex`, and atomics directly where they fit |
| `io` | Differential / contract-tested / Kotlin-shaped | Okio-native byte/character sources and sinks, data input/output, streaming operations, and injected-file-system support | Filesystem storage always takes Okio `FileSystem` and `Path`; no `java.io`/`java.nio.file` façade |
| `net` | Differential / contract-tested | IP literals, host/port parsing, transition addresses, public-suffix and domain logic | No JVM networking-object identity or scoped-address façade |
| `escape` | Differential | Char and Unicode escapers, safe ranges, replacement rules, malformed-surrogate handling | No allocation-strategy promise |
| math / primitives | Differential / contract-tested | Checked/saturated arithmetic, statistics, quantiles, `BigInteger`, scale-preserving `BigDecimal`, and Guava-shaped `Big*Math` operations | Common arbitrary-precision values are not complete `java.math` replacements: Java constructors, serialization, and JVM allocation/performance parity are out of scope |

## Arbitrary precision

`dev.guavakt.math.BigInteger` and `BigDecimal` are common immutable values, not JVM wrappers.
`BigInteger` supports arithmetic, radix and signed-byte conversion, two's-complement operations,
modular arithmetic, roots, and probable primes. `BigDecimal` supports scale-preserving arithmetic,
exact and rounded division, `MathContext`, point operations, plain/engineering rendering, square
roots, integral division, and exact finite-`Double` construction. The JVM gate directly compares
these value operations with `java.math` and compares `Big*Math` behavior with Guava where useful.

The values intentionally do not claim complete JDK API breadth, Java serialization, or JVM memory
and throughput equivalence. This is enough for Guava's core math utilities and KMP range domains;
it is not a promise to replace every `java.math.BigDecimal` call site.

## Explicit exclusions

The following are not omitted by accident and should not be reintroduced as compatibility shells:

- Java reflection, dynamic proxies, classpath scanning, `TypeToken`, and type-variable APIs;
- EventBus and ambient subscriber registries;
- `ListenableFuture`, executor, service, scheduled-executor, blocking-monitor, blocking-queue,
  and Java time-limiter APIs;
- `java.io`, `java.nio.file`, Java streams, and path bridges.

Use explicit dependencies, `KClass` or reified generics, typed `Flow`/`Channel` pipelines,
structured coroutines, and Okio instead.

## Permanent differences

1. Public packages are `dev.guavakt.*`; `com.google.common.*` is reserved for Guava and is never
   emitted by GuavaKt.
2. Kotlin stdlib replaces most List/Set/Map/Optional-style convenience APIs in new code.
3. Coroutines are the only asynchronous abstraction GuavaKt adds; Java concurrency abstractions
   are intentionally excluded.
4. Java serialization layout, private implementation classes, exact heap layout, and binary linkage
   to Guava are out of scope.
5. Weak/soft reference behavior and system filesystem availability are documented per target rather
   than emulated dishonestly.

## Verification

Parity claims require typed tests that execute both Guava/JDK and GuavaKt and compare values,
mutations, or exception behavior. Class counts and matching file names are never evidence of
behavioral compatibility.

```bash
./gradlew jvmTest :guavakt-parity:test --no-daemon
python3 scripts/hollow_inventory.py
python3 scripts/depth_inventory.py
python3 scripts/count_tests.py
python3 scripts/immutable_audit.py
```

Cross-platform CI also runs JS Node, Wasm Node, and Linux x64 tests, API checks, Dokka, and a
publication build. A capability is release-ready only when its appropriate gates are green.
