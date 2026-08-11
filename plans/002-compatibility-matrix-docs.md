# Plan 002: Compatibility matrix + README maturity bar

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: plans/001-depth-fidelity-metrics-gate.md (for linking depth metrics)
- **Category**: docs
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

Architecture score to **9** needs explicit consumer contracts: what is Guava-shaped vs Guava-compatible, per capability tier and platform. Without a matrix, users assume drop-in parity and hit silent ordering/reflect bugs.

## Current state

`README.md` today is minimal (~26 lines): status blurb, build command, usage coordinate `dev.guavakt:guavakt:0.1.0-SNAPSHOT`. Points to PARITY.md and DESIGN.md.

DESIGN.md documents tiers T0–T5 and non-goals (no binary compatibility with Guava JARs).

## Commands

| Purpose | Command | Expected |
|---------|---------|----------|
| Render check | `wc -l README.md PARITY.md` | README grows meaningfully |
| Build still works | `./gradlew :guavakt:compileKotlinJvm --no-daemon` | SUCCESS |

## Scope

**In scope:**
- `README.md` — expand with banner, matrix, depth metrics pointer, version policy
- `PARITY.md` — add compatibility matrix table if not duplicated
- `plans/README.md` status

**Out of scope:** Kotlin code, publishing credentials, Maven Central upload

## Steps

### Step 1: Rewrite README status section

Include exactly these sections (content accurate to repo):

1. **Banner** (blockquote): Not an official Google product. **Not binary-compatible with `com.google.guava:guava`.** Guava-shaped APIs and Maven coordinates use `dev.guavakt.*`. Behavioral compatibility is tiered — see matrix.

2. **Targets**: JVM, JS IR, Wasm JS, iOS, macOS, Linux x64, Mingw x64 (from DESIGN.md).

3. **Compatibility matrix** (markdown table):

| Area | JVM | JS / Native / Wasm | Notes |
|------|-----|--------------------|-------|
| Preconditions, Joiner, Immutable*, Multimaps (hash/list) | Full intent | Full intent | Prefer contract tests |
| Tree* / newTreeMap / newTreeSet | Sorted after plan 004 | Sorted after plan 004 | Historically LinkedHash* |
| Hash (Murmur, SHA*, Bloom) | Pure Kotlin | Pure Kotlin | Vector tests in guavakt-hash |
| Cache weak/soft | GC-backed | Strong stand-in | `platformSupportsWeakReferences()` |
| Files / NIO | actual | UnsupportedOperationException | |
| Reflection / Proxy | partial / Proxy actual | limited / UOE | |
| Monitor wait/condition | cooperative fidelity (plan 007) | cooperative | Not JVM ReentrantLock |

4. **Quality gates**: hollow, depth (`scripts/depth_inventory.py`), jvmTest.

5. **Build / test** — keep existing gradlew command; add `python3 scripts/depth_inventory.py`.

6. **Usage** — keep implementation line.

### Step 2: Cross-link PARITY.md

Ensure PARITY.md first lines still accurate; add “See README compatibility matrix.”

### Step 3: Update plans/README.md → DONE for 002

**Verify**: `grep -E "Not binary-compatible|Compatibility matrix|depth_inventory" README.md` all match.

## Done criteria

- [ ] README has non-compatible banner and matrix
- [ ] Depth and hollow gates mentioned
- [ ] No false claim of full Guava compatibility
- [ ] plans/README.md DONE

## STOP conditions

- DESIGN.md tier list conflicts with matrix — reconcile with DESIGN.md as source of truth for tiers

## Maintenance notes

Update matrix when plans 004–008 land (Tree*, Monitor, Reflect rows).
