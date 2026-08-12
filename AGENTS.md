# Using GuavaKt in a Kotlin Multiplatform project

This guide is for people and coding agents adding GuavaKt to an application, or forking it as a
starting point. It describes the product boundary; it is not a maintainer workflow.

## Install

Use the umbrella artifact when you want the complete library:

```kotlin
implementation("com.bernaferrari.guavakt:guavakt:0.1.0")
```

Use a focused artifact such as `guavakt-collect`, `guavakt-graph`, or `guavakt-hash` only when a
smaller dependency surface matters. See [README.md](README.md) for the module list and examples.

## Kotlin-first boundary

- Keep ordinary `List`, `Set`, `Map`, transformations, and nullable values in the Kotlin standard
  library.
- Use GuavaKt for Multimap, Multiset, BiMap, Table, Range collections, graphs, portable hashing,
  bounded caches, public-suffix utilities, exact math, and coroutine coordination.
- Use `kotlinx.coroutines` for application concurrency. GuavaKt provides coroutine-native cache
  loading, rate limiting, and guarded coordination—not futures, executors, services, or EventBus.
- Use Okio `FileSystem` and `Path` for storage boundaries. Do not introduce `java.io` or
  `java.nio.file` into shared code.
- Make hashing explicit with a `Funnel`; do not rely on reflection or JVM serialization.

The [Kotlin-first guide](docs/kotlin-first.md) explains these choices, and the
[compatibility matrix](docs/compatibility.md) records supported behavior and platform limits.

## Write portable code

Put shared application code in `commonMain`. GuavaKt's public APIs use
`com.bernaferrari.guavakt.*` and are designed for the project's declared JVM, JS, Wasm, and Native
targets. Pass platform capabilities such as an Okio filesystem or a coroutine scope explicitly.

For a cache, let a long-lived application or feature `CoroutineScope` own shared loads. For ranges,
use Kotlin's `0..10` and `0..<10` for simple loops; use `Range`, `RangeSet`, or `RangeMap` only
when boundary algebra or range collections are useful.

## Forking GuavaKt

Keep the Kotlin-first product boundary unless your fork has a concrete platform requirement.

- Preserve `commonMain` implementations whenever possible; use `expect`/`actual` only for genuine
  platform facilities.
- Keep the public package and published coordinate distinct from `com.google.guava`; GuavaKt is not
  binary-compatible with Guava.
- Preserve attribution for Guava-derived material and refresh the Public Suffix List through the
  documented review process.
- Treat weak references and filesystem availability as platform-specific capabilities, not portable
  guarantees.

## When changing this repository

Read [CONTRIBUTING.md](CONTRIBUTING.md) before modifying library code, tests, publication setup, or
compatibility claims. It contains the repository map, product rules, and verification gates.
