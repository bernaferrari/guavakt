# Benchmarks

Benchmarks are JVM-only measurement tools, not CI pass/fail gates. Correctness remains protected by
the deterministic and direct-Guava test suites; compare benchmark results only on the same machine,
JDK, Gradle command, and repository revision.

## Coroutine cache

Run the reproducible JMH workload suite with:

```bash
./gradlew :guavakt-cache:jmhCoroutineCache --no-daemon
```

The JSON report is written to `guavakt-cache/build/jmh-coroutine-cache.json`. The suite
reports throughput per millisecond for three controlled batches:

- `hotHitBatch`: 1,024 already-cached reads.
- `sameKeyMissBatch`: 64 simultaneous requests for one missing key. A loader barrier ensures every
  request joins the single in-flight load before it is released.
- `distinctKeyMissBatch`: 64 simultaneous requests for 64 missing keys. The barrier ensures every
  independent load starts before release.
- `evictingWriteBatch`: 512 writes into a 256-entry cache, exercising bounded-cache eviction and
  LRU maintenance from the same controlled state on each invocation.

Tune duration without changing source:

```bash
./gradlew :guavakt-cache:jmhCoroutineCache --no-daemon \
  -PjmhWarmupIterations=5 -PjmhMeasurementIterations=10 -PjmhForks=3
```

Do not impose a fixed throughput threshold in CI: results vary substantially with CPU governor,
thermal state, Java runtime, and coroutine dispatcher scheduling. Investigate statistically
meaningful regressions relative to a locally captured baseline, together with allocation/GC data
from a profiler when needed.

## Graph

```bash
./gradlew :guavakt-graph:jmhGraph --no-daemon
```

The JSON report is written to `guavakt-graph/build/jmh-graph.json`. The deterministic batches
separate sparse graph construction, reachability over a fixed 1,024-node DAG, and 1,024 alternating
edge mutations. They are intended to expose algorithmic or allocation regressions without turning a
machine-dependent throughput number into a CI gate.

## Hash

```bash
./gradlew :guavakt-hash:jmhHash --no-daemon
```

The JSON report is written to `guavakt-hash/build/jmh-hash.json`. It records 4 KiB Murmur3-128
one-shot hashing, Murmur3-128 streaming with 17-byte chunks, and SHA-256 streaming with the same
chunk boundaries. Keeping the cryptographic trace separate prevents an apparent Murmur improvement
from hiding a digest regression.

Every benchmark accepts the same optional duration controls shown above, for example
`-PjmhWarmupIterations=5 -PjmhMeasurementIterations=10 -PjmhForks=3`. Compare JSON reports only on
the same machine, JDK, workload settings, and revision.
