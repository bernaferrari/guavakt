# Contributing to GuavaKt

This guide is for anyone changing GuavaKt itself: people, coding agents, and maintainers. For
adding GuavaKt to an application or forking it for use in another project, start with
[AGENTS.md](AGENTS.md).

## First, understand the product

GuavaKt is a Kotlin-first Multiplatform library for useful Guava-shaped capabilities. It is not an
official Google project and does **not** replace `com.google.guava:guava` at source or binary level.
Public APIs use `com.bernaferrari.guavakt.*` so GuavaKt and Guava can coexist on the JVM.

Use Kotlin's standard library for ordinary `List`, `Set`, `Map`, transformations, and nullability.
Use `kotlinx.coroutines` for new asynchronous code. GuavaKt earns its place where Kotlin has no
common equivalent: multimaps, ranges, graphs, caches, hashing, public-suffix utilities, and
coroutine-aware coordination.

Read these documents in order when orienting yourself:

1. [README.md](README.md) — consumer-facing capability overview and examples.
2. [docs/README.md](docs/README.md) — index of current documentation.
3. [docs/kotlin-first.md](docs/kotlin-first.md) — API choices and migration boundary.
4. [docs/architecture.md](docs/architecture.md) — module DAG and platform policy.
5. [docs/compatibility.md](docs/compatibility.md) — evidence, limitations, and intentional differences.
6. [docs/roadmap.md](docs/roadmap.md) — current priority order and definition of done.

## Repository map

`settings.gradle.kts` lists every module. Production code is split by capability; the umbrella
artifact is `com.bernaferrari.guavakt:guavakt` and `guavakt-parity` is the JVM oracle/differential test module.

| Area | Modules | Purpose |
|---|---|---|
| Foundations | `annotations`, `base`, `primitives`, `math`, `escape` | Shared values, utilities, arithmetic, and escaping |
| Data structures | `collect`, `graph` | Multimaps, multisets, tables, ranges, immutable views, and graphs |
| Runtime utilities | `hash`, `cache`, `io`, `net` | Digests, Bloom filters, caches, Okio I/O, networking, and PSL data |
| Coordination | `concurrent` | Coroutine-native rate limiting and guarded synchronization |
| Verification | `parity` | Direct Guava/JDK differential and long-running oracle tests |

Physical source paths follow Guava's package layout for ease of comparison, while Kotlin package
names remain `com.bernaferrari.guavakt.*`. Do not infer a compatibility promise from a familiar filename: public
contract and tests are what matter.

## Where implementation belongs

Put algorithms and normal data-structure behavior in `src/commonMain` by default. That is where
almost all production code lives. `jvmMain` is deliberately small and reserved for a facility that
cannot exist honestly on every target:

- GC-backed weak, soft, and phantom references.

An `expect`/`actual` split is not a convenience mechanism. Before adding one, ask whether an
injected Okio `FileSystem`, a coroutine API, a Kotlin value type, or an explicit unsupported result
would be a more portable and honest design. Never move a pure algorithm into `jvmMain` merely to
reuse a JDK class or make a JVM test easier.

## Product rules

1. **Kotlin-first.** Prefer stdlib collections and `T?`; keep Guava names when the underlying
   concept is genuinely useful, such as `Range`, `Multimap`, `Graph`, `Cache`, or `Hashing`.
2. **Coroutines first.** New asynchronous APIs use suspension, `Flow`, `Duration`, and explicit
   scope ownership. Do not add Guava-style futures, executors, services, blocking waits, or
   reflection/proxy compatibility layers.
3. **Immutable means immutable.** `Immutable*` values are snapshots. They must expose read-only
   Kotlin interfaces or reject every mutation route, including iterators, entries, inverse views,
   and subviews. Never use `AbstractMutable*` with a live `put` implementation.
4. **Live views keep bookkeeping correct.** Mutating a supported view such as `keys`, `values`,
   `entries`, `asMap`, a sublist, or an inverse must update the parent correctly.
5. **Platform limits are visible.** Document differences involving GC, filesystems, and timers in
   KDoc and in the compatibility matrix. Do not emulate a capability dishonestly with a busy loop
   or a mutable stand-in.
6. **Compatibility is behavioral evidence.** Class names, source counts, and “looks like Guava” are
   not parity. A claim needs typed direct-oracle tests or a documented Kotlin-shaped contract.

## A safe change loop

1. Inspect the owning module, its tests, and the relevant row in
   [docs/compatibility.md](docs/compatibility.md).
2. Decide whether Kotlin stdlib/coroutines already solve the problem. If so, avoid adding a shim.
3. Implement in `commonMain` unless a real platform boundary requires an `actual`.
4. Add focused common tests. For Guava-compatible behavior, add or extend JVM parity tests that
   compare values, mutations, and failures—not only happy-path outputs.
5. Update KDoc and the compatibility matrix whenever behavior or a tier changes. Update the README
   only when the consumer-facing promise changes.
6. Keep the worktree scoped. Existing changes belong to their author; do not stage, discard, or
   rewrite unrelated files.

## Verification required before a claim of completion

Run the complete JVM/oracle and structural gates for a meaningful implementation change:

```bash
./gradlew jvmTest :guavakt-parity:test --no-daemon
python3 scripts/hollow_inventory.py   # TOTAL_HOLLOW=0
python3 scripts/depth_inventory.py    # PCT_OK_OR_DEEP=100%, no wrong-kind
python3 scripts/count_tests.py        # meets MIN_TESTS
python3 scripts/immutable_audit.py
```

For portable code, also run the smallest relevant JS/Wasm/Native task or the repository CI target.
For numeric or range work, use the reproducible long-run controls in
[docs/fuzzing.md](docs/fuzzing.md). Benchmark algorithmically sensitive cache, graph, or hash work
with [docs/benchmarks.md](docs/benchmarks.md), but never turn machine-dependent throughput into a
CI threshold.

The inventory and depth scripts are structural guardrails, not proof of Guava parity. Treat a
failed direct oracle as a release blocker unless the discrepancy is explicitly documented as a
Kotlin-shaped or platform-limited behavior.

## Documentation of record

- [docs/compatibility.md](docs/compatibility.md) is the authoritative statement of supported
  behavior and deliberate differences.
- [docs/architecture.md](docs/architecture.md) owns dependency and platform policy.
- [docs/roadmap.md](docs/roadmap.md) is the only current planning source; do not revive deleted
  snapshot plans as instructions.
- [docs/public-suffix.md](docs/public-suffix.md) owns PSL refresh discipline.
- [docs/releasing.md](docs/releasing.md) owns Maven Central prerequisites and release checks.

Keep the repository root small: `README.md` for consumers, `AGENTS.md` for downstream users and
forks, and this file for contributors.
Put lasting reference material in `docs/` and sample-specific documentation next to its sample.

## Do not

- Claim binary compatibility with `com.google.guava:guava` or publish `com.google.common.*`.
- Replace `kotlinx.coroutines` with Guava-style futures as the primary asynchronous API.
- Add Java reflection, dynamic-proxy, EventBus, executor, service, or blocking-queue APIs.
- Add hollow templates, bag types, or `TODO: implement` to production source.
- Hide JVM dependencies in common APIs or silently grant an early permit, fake a blocking timeout,
  or pretend a weak reference works on every target.
- Treat stale plans, test counts, class inventory, or compilation alone as behavioral completion.
