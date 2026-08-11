# Plan 003: JVM Guava differential test harness

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED
- **Depends on**: none (can land parallel to 001)
- **Category**: tests
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

Test maturity and fidelity to **9** require proving GuavaKt matches Guava **on JVM** for shared contracts. A differential harness runs the same inputs against `com.google.guava:guava` and GuavaKt types and asserts equality. This is the highest-leverage test investment; plans 004–012 use it as a regression net.

## Current state

- GuavaKt modules now use `dev.guavakt.*`; official Guava can be a normal test dependency without a package clash.
- The existing isolated loader may be retained for reflective matrix generation, but new differential tests should prefer compile-time type aliases and direct calls.

**Recommended approach (pick this):** new module `guavakt-guava-diff` that:
1. Depends on Guava JAR normally (`com.google.guava:guava:33.4.0-jre` or whatever version matches `guava-upstream` pom).
2. Depends on GuavaKt modules **only via a shaded test jar** OR — simpler for KMP — **does not depend on GuavaKt artifacts by package**; instead runs as pure JUnit in `guavakt` with **goldens checked in**.

**Simpler approach that avoids classloader hell (PREFER):**

Create `guavakt-diff-goldens` generation is optional. Implement **`guavakt-jvm-parity`** as a **JVM-only** source set tests inside each module that assert Guava contracts without loading Guava — using **known vectors** already in `GuavaVectorFidelityTest` style.

AND create module **`guava-oracle-tests`** (JVM only, Java or Kotlin) that **only** depends on Guava and writes nothing to GuavaKt — used to **regenerate** golden JSON under `guavakt-*/src/jvmTest/resources/goldens/`. GuavaKt tests load goldens and compare.

Even simpler for executor success:

**Module `:guavakt-parity`** (JVM only Kotlin):
- `implementation(project(":guavakt"))` — GuavaKt packages
- **No Guava dependency**
- Tests encode contracts from Guava documentation / existing Guava tests ported by hand for critical APIs
- Name it differential because assertions are copied from Guava behavior

Plus optional second module with Guava if executor can use `classpath` isolation:

Use Java `URLClassLoader` loading Guava JAR from Gradle configuration `guavaOracle` without exporting packages to compile classpath of GuavaKt code — only the test class loads Guava reflectively:

```kotlin
// Pseudocode
val loader = URLClassLoader(arrayOf(guavaJar.toURI().toURL()), null)
val preconditions = loader.loadClass("com.google.common.base.Preconditions")
// invoke checkArgument and compare exception messages with GuavaKt Preconditions
```

Parent classloader null avoids seeing GuavaKt's `com.google.common`. GuavaKt loaded from app classloader. This **works** if Guava JAR path is known.

## Commands

| Purpose | Command | Expected |
|---------|---------|----------|
| Guava version from upstream | `grep -A2 '<artifactId>guava</artifactId>' guava-upstream/guava/pom.xml \| head -5` or read parent pom | version string |
| Tests | `./gradlew :guavakt-parity:test --no-daemon` or `:guavakt:jvmTest` | SUCCESS |
| Full | `./gradlew jvmTest --no-daemon` | SUCCESS |

## Scope

**In scope:**
- New module `guavakt-parity` (JVM-only) OR `guavakt/src/jvmTest` package `dev.guavakt.parity`
- `settings.gradle.kts` include if new module
- Gradle config for Guava JAR as `guavaOracle` files dependency for classloader tests
- At least **200** assertions across: Preconditions messages, Optional, Joiner/Splitter, ImmutableList/Set/Map factories, Multimap put/get/removeAll live views, HashMultiset counts, Ints/Longs, IntMath, Murmur3_32 known vectors (delegate to existing if present), Futures.immediateFuture get, CacheBuilder maximumSize

**Out of scope:**
- Changing GuavaKt production algorithms (unless a test reveals a one-line bug — then fix with minimal patch and note in plan status)
- Full guava-tests port (plan 009)
- JS/Native

## Steps

### Step 1: Add module or jvmTest package

Prefer **new module** `guavakt-parity` for clarity:

`settings.gradle.kts` add `:guavakt-parity`.

`guavakt-parity/build.gradle.kts`:
```kotlin
plugins { kotlin("jvm") } // or multiplatform jvm-only
dependencies {
  implementation(project(":guavakt"))
  testImplementation(kotlin("test"))
  // Guava only for oracle classloader — not compile dependency of main
  testRuntimeOnly("com.google.guava:guava:33.4.0-jre") // align version with guava-upstream
}
```

If `kotlin("jvm")` not used elsewhere, use multiplatform with only `jvm()` and no commonMain code.

### Step 2: Implement `OracleClassLoader` helper

`guavakt-parity/src/test/kotlin/dev/guavakt/parity/GuavaOracle.kt`:
- Resolves Guava JAR from classpath resources / system property
- `loadGuavaClass(name: String): Class<*>`
- Helpers: `invokeStatic`, compare lists/maps

If classloader approach fails twice, **STOP** and fall back to golden-vector-only tests without Guava JAR (still ≥200 assertions from Guava documentation values).

### Step 3: Write parity test classes

Minimum files:
- `PreconditionsParityTest.kt` — badElementIndex message format vs GuavaKt (and Guava if oracle works)
- `ImmutableCollectionsParityTest.kt`
- `MultimapLiveViewParityTest.kt` — already partially in MultimapLiveViewTest; extend
- `HashParityTest.kt` — empty string MD5, etc. (may already exist in guavakt-hash; add cross-module)
- `FuturesParityTest.kt`
- `CacheParityTest.kt` — maximumSize eviction order

Each test method: arrange inputs → act on GuavaKt → assert expected Guava behavior.

### Step 4: Run and fix only trivial GuavaKt bugs

If GuavaKt fails an assertion that is clearly a bug (wrong message, multimap live view), fix the production code in the relevant module with minimal change. If failure is known KMP limit (Tree order), mark test `// PLAN004` and use `expect fails` or exclude until 004 — document in test name `treeMultimap_ordering_pending004`.

### Step 5: Document in README / PARITY how to run parity module

### Step 6: plans/README.md DONE

**Verify**: Count tests: `./gradlew :guavakt-parity:test` or grep `@Test` in new files ≥ 40 methods with ≥ 200 assert* calls total (`rg -c 'assert' guavakt-parity`).

## Done criteria

- [ ] Parity module or package exists and runs on CI path (`jvmTest` includes it)
- [ ] ≥ 200 assertions; all passing (pending004 exclusions allowed ≤ 20)
- [ ] Guava oracle classloader **or** documented golden fallback
- [ ] `./gradlew jvmTest` SUCCESS
- [ ] hollow still 0
- [ ] plans/README.md DONE

## STOP conditions

- Package clash prevents compilation — do not relocate GuavaKt packages; use classloader or goldens only
- Guava version cannot be determined — use `33.4.0-jre` and note in README

## Maintenance notes

Plan 009 ports more Guava tests into GuavaKt commonTest; parity module stays JVM-oracle focused.
