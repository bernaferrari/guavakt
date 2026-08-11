# Reproducible long-running fuzzing

The direct-Guava parity tests use deterministic seeds and include arbitrary-precision arithmetic plus RangeSet traces. Normal parity runs execute 12 seeds × 96 cases, which is substantial enough to cover routine regression detection without making every build an overnight job.

For a prolonged JVM oracle run, increase both controls explicitly:

```bash
./gradlew :guavakt-parity:test \
  --tests dev.guavakt.parity.BigIntegerMathDifferentialTest \
  --tests dev.guavakt.parity.RangeDifferentialTest \
  -PfuzzSeeds=128 -PfuzzCases=1024 \
  --no-daemon -Pkotlin.incremental=false
```

The command replays 131,072 decimal arithmetic cases (addition, subtraction, multiplication, division/remainder, one rounded division, square root, and log₂) against the JDK and Guava. It also performs 131,072 arbitrary-precision `TreeRangeSet` operations against Guava after every mutation, including complements, containment probes, subrange views, enclosure, and intersection checks.

Every failure reports its seed, case/step, operation, and operands/range. Reproduce just one seed by using `-PfuzzSeeds=1` after changing the test's initial seed locally, or keep the default deterministic sequence and narrow `-PfuzzCases` while investigating the reported position. These tests are JVM oracle tests by design; the portable `commonTest` suite separately compiles and exercises the implementation on JVM, JS, Wasm, and supported Native targets.
