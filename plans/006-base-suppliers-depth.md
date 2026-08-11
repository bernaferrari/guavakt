# Plan 006: Deepen base utilities (Suppliers + peers)

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: plans/003-jvm-guava-diff-harness.md
- **Category**: tech-debt
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

`Suppliers.kt` is 24 lines vs Guava ~456. Missing `memoizeWithExpiration`, thread-safe memoization, `supplierFunction`, `compose`. Base module fidelity is foundational for architecture trust.

## Current state

```kotlin
object Suppliers {
    fun <T> ofInstance(instance: T): Supplier<T> = Supplier { instance }
    fun <T> memoize(delegate: Supplier<T>): Supplier<T> = object : Supplier<T> {
        private var initialized = false
        private var value: Any? = UNSET
        override fun get(): T {
            if (!initialized) {
                value = delegate.get()
                initialized = true
            }
            return value as T
        }
    }
    private val UNSET = Any()
}
```

Guava also has synchronized memoization for races — on KMP use `@Synchronized` or `Mutex`-free synchronized via `monitorSync` / platform lock if available in base module (`synchronized` in Kotlin/JVM; on other targets use atomic ref + double-check with a lock `Any()` and platform expect if needed). Prefer `kotlin.concurrent` only if multiplatform-safe — use a simple lock `private val lock = Any()` with `synchronized(lock)` **JVM** and on common use the same pattern GuavaKt already uses in concurrent (`monitorSync`). Check if `monitorSync` is in base or only concurrent — if only concurrent, implement local `synchronized`-like in Suppliers with expect/actual or use atomic `var` with accept-race documentation matching Guava’s non-threadsafe memoize vs `memoize` synchronized variant.

Port from `guava-upstream/guava/src/com/google/common/base/Suppliers.java`:
- `ofInstance`
- `memoize` (thread-safe)
- `memoizeWithExpiration(supplier, duration, unit)` — use `Ticker` or `kotlin.system.getTimeNanos` / GuavaKt `Ticker.systemTicker()`
- `synchronizedSupplier`
- `supplierFunction` (Function that calls supplier)
- `compose(Function, Supplier)`

Also deepen if still thin (quick wins in same PR if &lt; 1 hour each):
- `Throwables` — already partially done; skip if OK
- `Enums` — KMP limited; skip Proxy

## Scope

**In scope:**
- `guavakt-base/src/commonMain/.../Suppliers.kt`
- `guavakt-base/src/commonTest/.../SuppliersTest.kt` (new)
- Optional parity assertions in guavakt-parity

**Out of scope:** Full rewrite of all base types

## Steps

### Step 1: Port Suppliers API to Guava-shaped methods

Match Guava method names and signatures as closely as KMP allows (`Long` nanos for expiration if TimeUnit absent — use `expirationNanos: Long` + overload with seconds if Guava uses TimeUnit: define `fun memoizeWithExpiration(delegate: Supplier<T>, duration: Long, unit: /* use Long millis */)` — check GuavaKt for existing `TimeUnit` or use milliseconds long).

If no TimeUnit in project, use:
`fun <T> memoizeWithExpiration(delegate: Supplier<T>, durationNanos: Long, ticker: Ticker = Ticker.systemTicker())`

### Step 2: Tests — memoize once, expiration refreshes, ofInstance identity

### Step 3: plans/README.md DONE

**Verify**: `./gradlew :guavakt-base:jvmTest`; Suppliers.kt line count &gt; 100.

## Done criteria

- [ ] memoizeWithExpiration + synchronizedSupplier + compose present
- [ ] SuppliersTest passes
- [ ] hollow 0

## STOP conditions

- Ticker not available in base module — add dependency or use `kotlin.system.getTimeNanos()` only for expiration

## Maintenance notes

Thread-safety tests on JVM only optional.
