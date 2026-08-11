# Plan 010: CI multiplatform compile + quality gates

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: plans/001-depth-fidelity-metrics-gate.md, plans/009-characterization-test-corpus.md
- **Category**: dx
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

Architecture **9** and maturity **9** require CI that enforces hollow, depth baseline, min tests, full portable JS compile, and at least one non-JVM test target smoke.

## Current state

`.github/workflows/ci.yml`:
- `jvm-test`: jvmTest + hollow_inventory.py
- `js-compile`: only base, primitives, collect, hash, math

Missing: depth gate, test count, reflect/cache/concurrent/escape/graph/io/net/eventbus JS compile, non-JVM tests.

## Scope

**In scope:**
- `.github/workflows/ci.yml`
- Possibly `gradle.properties` for CI memory
- Ensure scripts from 001/009 exist (if not, implement minimal stubs that fail with message to run those plans first)

**Out of scope:**
- Publishing
- Windows CI matrix (optional)

## Steps

### Step 1: Expand js-compile job

Compile JS for **all** modules that declare `js(IR)`:
```
:guavakt-annotations:compileKotlinJs
:guavakt-base:compileKotlinJs
:guavakt-primitives:compileKotlinJs
:guavakt-math:compileKotlinJs
:guavakt-collect:compileKotlinJs
:guavakt-escape:compileKotlinJs
:guavakt-hash:compileKotlinJs
:guavakt-graph:compileKotlinJs
:guavakt-cache:compileKotlinJs
:guavakt-io:compileKotlinJs
:guavakt-net:compileKotlinJs
:guavakt-eventbus:compileKotlinJs
:guavakt-concurrent:compileKotlinJs
:guavakt-reflect:compileKotlinJs
:guavakt:compileKotlinJs
```

If a module fails due to missing actuals, fix minimal actuals (STOP if large design issue).

### Step 2: Add gates to jvm-test job

```yaml
      - name: Hollow inventory gate
        run: python3 scripts/hollow_inventory.py
      - name: Depth inventory gate
        run: python3 scripts/depth_inventory.py
      - name: Test count gate
        run: python3 scripts/count_tests.py
```

### Step 3: Add js-test or linux-native smoke job

Prefer:
```yaml
  js-test-smoke:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: ./gradlew :guavakt-base:jsNodeTest :guavakt-primitives:jsNodeTest --no-daemon
```

If js tests fail due to infra, use `linuxX64Test` for base+primitives only on ubuntu.

### Step 4: Document CI in README

### Step 5: plans/README.md DONE

**Verify**: Act locally if available; else `python3` scripts + `./gradlew compileKotlinJs` for all modules.

## Done criteria

- [ ] CI YAML includes depth + count + hollow
- [ ] All portable modules compile to JS in CI
- [ ] At least one non-JVM test task in CI
- [ ] Local scripts exit 0

## STOP conditions

- Native toolchain missing on GHA — prefer JS node tests
- Module has no js target — skip that module only

## Maintenance notes

Bump min tests in count_tests.py as corpus grows.
