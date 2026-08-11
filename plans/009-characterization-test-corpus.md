# Plan 009: Characterization test corpus (≥800 tests)

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/003, 004, 006, 007 (partial corpus OK earlier; done criteria needs ≥800 after deps)
- **Category**: tests
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

Test maturity **9** needs broad automated coverage (~800+ tests), not 152 smoke tests. Port **characterization** tests from Guava (behavior locks) into GuavaKt `commonTest` / `jvmTest`, adapted to Kotlin test (`kotlin.test`). Prefer many small tests over few giant ones.

## Current state

- ~152 jvm tests across modules, all passing historically
- Upstream: `guava-upstream/guava-tests/test/com/google/common/**/**/*Test.java` (~585 files, ~52k LOC)
- Existing style: `CollectParityTest.kt`, `GuavaVectorFidelityTest.kt`, `SkepticDefiningApiTest.kt`

## Scope

**In scope:**
- New and expanded `*Test.kt` under each `guavakt-*/src/commonTest` and `jvmTest` where needed
- Script optional `scripts/count_tests.py` printing total `@Test` methods
- Do **not** copy Guava’s JUnit4 / Truth / Guava testlib dependencies — adapt to `kotlin.test`

**Out of scope:**
- Porting Guava testlib framework
- Android-specific tests

## Steps

### Step 1: Measure baseline

```bash
rg -c '@Test' guavakt*/src --glob '*.kt' | awk -F: '{s+=$2} END {print s}'
```
Record N0 (~150–200).

### Step 2: Priority suites to port/adapt (order)

For each, read Guava test for cases, write Kotlin tests asserting GuavaKt behavior.

| Priority | Module | Focus | Target new @Test methods |
|----------|--------|-------|---------------------------|
| 1 | collect | ImmutableList/Set/Map, Multimap, Multiset, Iterables, Lists, Maps, Sets, Range, Table | +200 |
| 2 | base | Preconditions, Optional, Joiner, Splitter, CharMatcher, Suppliers, Throwables | +80 |
| 3 | primitives | Ints, Longs, Bytes, Unsigned* | +60 |
| 4 | hash | all HashFunction vectors, BloomFilter | +40 |
| 5 | concurrent | Futures, AbstractService, Monitor (JVM), RateLimiter | +80 |
| 6 | cache | CacheBuilder, LoadingCache, removal | +40 |
| 7 | graph | Graph/ValueGraph/Network | +40 |
| 8 | math | IntMath, LongMath, Stats, Quantiles | +40 |
| 9 | net/io/escape/eventbus | smoke + key contracts | +40 |
| 10 | parity module | expand | +40 |

Total new ≥ 650 → overall ≥ 800.

### Step 3: Implementation rules

- Package names match production (`dev.guavakt.collect`)
- Use `kotlin.test.Test`, `assertEquals`, `assertTrue`, `assertFailsWith`
- When Guava uses checked exceptions, adapt
- Skip `@GwtIncompatible` only tests if not applicable
- If GuavaKt intentionally diverges, assert **documented** GuavaKt behavior and link PARITY.md — do not assert wrong Guava behavior silently

### Step 4: Add `scripts/count_tests.py`

Prints total `@Test` fun count; exit 1 if &lt; 800 (for CI in plan 010). Locally allow env `GUAVAKT_MIN_TESTS=800`.

### Step 5: Run full jvmTest until green

Fix GuavaKt bugs found only when clear and small; otherwise file comment `// FIDELITY: known gap` and assert current behavior with link to future plan — **prefer fixing** if under 20 lines.

### Step 6: plans/README.md DONE

**Verify**: `python3 scripts/count_tests.py` ≥ 800; `./gradlew jvmTest` SUCCESS.

## Done criteria

- [ ] ≥ 800 `@Test` methods in guavakt* sources
- [ ] All jvmTest pass
- [ ] count_tests.py exists
- [ ] hollow 0

## STOP conditions

- Mass failures after port — bisect module by module
- Guava test depends on testlib Testers — rewrite as direct assertions instead of framework

## Maintenance notes

Add tests with every new depth PR. Plan 010 wires min test count to CI.
