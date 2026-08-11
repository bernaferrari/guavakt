# Plan 007: Concurrent Monitor and timed-wait fidelity

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: HIGH
- **Depends on**: plans/003-jvm-guava-diff-harness.md
- **Category**: tech-debt
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

`Monitor.enterWhen` currently throws if the guard is unsatisfied instead of waiting — breaks Guava’s primary Monitor use case. Fidelity **9** needs cooperative waiting on commonMain (spin/park with timeout using platform scheduler expect/actual where available) matching Guava semantics for single-threaded tests and multi-threaded JVM tests.

## Current state

`Monitor.kt`:
```kotlin
fun enterWhen(guard: Guard) {
    require(guard.monitor === this)
    enter()
    if (!guard.isSatisfied()) {
        leave()
        throw IllegalStateException("Guard not satisfied (KMP monitor cannot wait)")
    }
}
```

Guava Monitor uses ReentrantLock + Condition. GuavaKt has `PlatformScheduler` / `monitorSync` in concurrent module — read those files and reuse.

## Scope

**In scope:**
- `Monitor.kt`, possibly `MonitorSync.kt`, `PlatformScheduler.kt` (+ expect/actual if needed)
- `Uninterruptibles` deepen if still thin
- `TimeoutFuture` if relies on scheduler
- Tests: `MonitorTest.kt` on **jvmTest** for multi-thread; commonTest for cooperative single-thread signal pattern

**Out of scope:**
- Perfect fairness vs Guava on non-JVM
- CycleDetectingLockFactory full rewrite

## Steps

### Step 1: Design cooperative wait

On JVM: use `synchronized` + `Object.wait`/`notifyAll` via actual, or `java.util.concurrent.locks.ReentrantLock` in jvmMain actual for Monitor.

**Preferred for fidelity on JVM:** `expect class Monitor` is wrong (API must stay common class). Instead:
- Keep `Monitor` in commonMain
- Use `internal expect fun monitorWait(lock: Any, nanos: Long): Boolean` and `monitorNotifyAll(lock: Any)` 
- jvmMain: `lock.wait` / `notifyAll`
- js/native/wasm: busy-spin with yield or throw on timed wait unsupported — **document**; for JS use continuations only if already in project

If expect wait is too large, implement JVM-correct Monitor in common using only `kotlinx.atomicfu` — check if atomicfu is a dependency. If not, add for concurrent module only **or** use synchronized on JVM via:

```kotlin
// commonMain
internal expect inline fun <T> platformSynchronized(lock: Any, block: () -> T): T
internal expect fun platformWait(lock: Any, timeoutMillis: Long)
internal expect fun platformNotifyAll(lock: Any)
```

### Step 2: Reimplement enterWhen / enterWhenUninterruptibly / tryEnterWhen

Match Guava:
- enter lock
- while (!guard.isSatisfied()) wait
- on signal, re-check

Leave releases and notifyAll.

### Step 3: JVM tests with two threads

Thread A waits on guard; Thread B sets state and leaves; A proceeds. Timeout tests.

### Step 4: Non-JVM: best-effort spin with max iterations then throw with clear message if still unsatisfied (or implement wait on native with NSCondition — only if straightforward)

Document in KDoc.

### Step 5: plans/README.md DONE

**Verify**: `./gradlew :guavakt-concurrent:jvmTest`; no throw on satisfied-after-wait scenario on JVM.

## Done criteria

- [ ] `enterWhen` waits instead of immediately throwing when guard currently false (JVM multi-thread test passes)
- [ ] Fairness optional
- [ ] hollow 0
- [ ] KDoc describes non-JVM behavior

## STOP conditions

- Cannot add expect/actual without breaking compile on all targets — fix each target’s actual
- Deadlocks in tests — fix implementation

## Maintenance notes

High risk — prefer small Monitor surface tests before touching Futures.
