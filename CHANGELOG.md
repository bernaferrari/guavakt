# Changelog

All notable changes are documented here. GuavaKt follows semantic versioning within its declared
Kotlin-first scope; see the [compatibility matrix](docs/compatibility.md) for supported behavior.

## 0.1.0

### Added

- A focused Kotlin Multiplatform core: rich collections and ranges, graphs, hashes and Bloom
  filters, caching, Okio-based I/O, network/public-suffix utilities, and arbitrary-precision math.
- Coroutine-native `CoroutineRateLimiter` and `CoroutineMonitor`, with cancellation and virtual-time
  coverage across JVM, JS, Wasm, and Native-compatible source sets.
- Direct JDK/Guava verification for scale-sensitive `BigDecimal` square roots, integral division,
  and exact finite-`Double` construction.
