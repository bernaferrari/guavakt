# Plan 011: LocalCache / MapMaker parity suite

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/003, plans/009
- **Category**: tests / tech-debt
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

Guava `LocalCache` is ~5000 LOC; GuavaKt ~341 LOC with real LRU/TTL/weak paths but incomplete Guava semantics (segments, refresh, eviction concurrency). Fidelity **9** for cache needs locked contracts via tests and fixes for gaps found (maximumSize, expireAfterWrite, expireAfterAccess, removalListener, stats, loading, weak values on JVM).

## Current state

`LocalCache.kt` implements LoadingCache with strongMap, weakKeyBuckets, stats, cleanUp. `WeakSoftCacheTest.kt` and `CacheTest.kt` exist with few tests.

`MapMaker` strong only on KMP with weak API names.

## Scope

**In scope:**
- `guavakt-cache` production fixes required by new tests (minimal)
- Expand `CacheTest.kt`, `WeakSoftCacheTest.kt`, add `LocalCacheParityTest.kt`
- `CacheBuilder` / `CacheBuilderSpec` edge cases

**Out of scope:**
- Full Guava segment striping
- Soft ref exact GC timing tests (flaky) — only structural tests

## Steps

### Step 1: Write parity tests (must pass or fix code)

- maximumSize=1 evicts eldest on second put
- expireAfterWrite with fake Ticker advances and miss
- expireAfterAccess with fake Ticker
- removalListener invoked with correct RemovalCause (SIZE, EXPIRED)
- stats hit/miss counts
- CacheLoader load on get
- weakValues on JVM: put, null out, GC + cleanUp — **optional flaky**; use Platform refs mock if possible
- CacheBuilderSpec parse maximumSize and expire

### Step 2: Fix LocalCache gaps

Common gaps: access expiry not applied on get; removal listener not fired; loading exception handling. Fix to match Guava contracts under test.

### Step 3: MapMaker

Tests for strong keys/values; weakKeys API does not throw; document strong on non-JVM.

### Step 4: plans/README.md DONE

**Verify**: `./gradlew :guavakt-cache:jvmTest`

## Done criteria

- [ ] ≥ 25 cache-related @Test methods
- [ ] All pass
- [ ] No known silent no-op for maximumSize/expire on common path

## STOP conditions

- GC tests flaky — mark `@Ignore` on non-deterministic and test cleanUp API with manual Platform ref clear if hooks exist

## Maintenance notes

Cache concurrency stress tests optional later.
