# Changelog

All notable changes are documented here. GuavaKt follows semantic versioning within its declared
Kotlin-first scope; see the [stability policy](docs/stability.md) for the compatibility contract.

## 0.1.0

### Added

- A focused Kotlin Multiplatform core: rich collections and ranges, graphs, hashes and Bloom
  filters, caching, Okio-based I/O, network/public-suffix utilities, and arbitrary-precision math.
- Coroutine-native `CoroutineRateLimiter` and `CoroutineMonitor`, with cancellation and virtual-time
  coverage across JVM, JS, Wasm, and Native-compatible source sets.
- Direct JDK/Guava verification for scale-sensitive `BigDecimal` square roots, integral division,
  and exact finite-`Double` construction.

### Changed

- Filesystem APIs are explicitly Okio `FileSystem` plus `Path`; string-path and JVM filesystem
  bridges are not part of the public surface.
- Public API baselines, compatibility documentation, release instructions, support policy, and
  README now describe the maintained KMP scope.

### Removed intentionally

- Java reflection/proxy/classpath APIs, EventBus, `ListenableFuture`, executors, services, blocking
  queues/monitors, time limiters, fake synchronization/concurrency adapters, and JVM
  `InputStream`/`Reader` bridge overloads.

These removals are deliberate breaking changes made before the first release. Use
coroutines, typed `Flow`/`Channel` pipelines, explicit dependencies, and Okio instead.
