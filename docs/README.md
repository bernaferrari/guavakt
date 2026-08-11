# GuavaKt documentation

This directory holds the documentation of record. The repository root stays intentionally small:
start with the [project README](../README.md), while [`AGENTS.md`](../AGENTS.md) explains the
repository, engineering rules, and change workflow for contributors and automated agents.

## Product and consumer guidance

- [Compatibility matrix](compatibility.md) — evidence tiers, supported behavior, and intentional KMP differences.
- [Kotlin-first guide](kotlin-first.md) — when to prefer Kotlin, Okio, or coroutines over a Guava-shaped API.
- [Architecture](architecture.md) — module dependency graph and platform policy.
- [Roadmap](roadmap.md) — current priority order and the definition of done for a capability.

## Maintainer references

- [Fuzzing](fuzzing.md) — reproducible long-running JVM oracle runs.
- [Benchmarks](benchmarks.md) — JMH workloads and comparison rules.
- [Public Suffix List refresh](public-suffix.md) — offline data-refresh discipline.
- [Releasing](releasing.md) — Central Portal prerequisites and release checks.
