# Plan 001: Add depth/fidelity metrics gate (beyond hollow)

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving on. If anything in STOP conditions occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: Compare live `scripts/hollow_inventory.py` and `scripts/port_status.py` to excerpts below. On mismatch that changes classification semantics, STOP.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx / tech-debt
- **Planned at**: workspace snapshot 2026-06-28 (no git SHA in tree)

## Why this matters

`TOTAL_HOLLOW=0` only proves types are not wrong-kind bags. Architecture maturity at **9** needs a second gate that tracks **implementation depth** (THIN vs OK vs DEEP) so regressions and prioritization are visible in CI. Without this, fidelity work has no measurable definition of done.

## Current state

- `scripts/hollow_inventory.py` — structural hollow gate; prints `TOTAL_HOLLOW=0` on success.
- `scripts/port_status.py` — prints `files=610 likely_thin=345 okish=265` using line-count heuristics (`likely_thin` if small LOC).
- `build/hollow-summary.txt` — implementer scratch with `TOTAL_HOLLOW=0`.
- `PARITY.md` documents hollow=0 but no tier percentages.

`port_status.py` heuristic (conceptual): small files → thin; does not write machine-parseable exit codes or tier JSON.

Conventions: Python 3 scripts at repo root `scripts/`, invoked from CI as `python3 scripts/hollow_inventory.py`. Kotlin modules under `guavakt-*/src/commonMain/kotlin/com/google/common/`.

From DESIGN.md: capability tiers T0–T5; this plan adds **depth tiers** orthogonal to capability tiers.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Hollow gate | `python3 scripts/hollow_inventory.py` | exit 0, contains `TOTAL_HOLLOW=0` |
| Port status | `python3 scripts/port_status.py` | exit 0, prints counts |
| New depth gate | `python3 scripts/depth_inventory.py` | exit 0 when thresholds met |
| JVM tests | `./gradlew jvmTest --no-daemon` | BUILD SUCCESSFUL |

## Scope

**In scope:**
- `scripts/depth_inventory.py` (create)
- `scripts/port_status.py` (extend to emit tiers or call shared logic — prefer thin wrapper)
- `scripts/hollow_inventory.py` (optional: print pointer to depth script; do not weaken hollow checks)
- `PARITY.md` — add “Depth tiers” section
- `plans/README.md` — status row
- `.github/workflows/ci.yml` — **only if** plan 010 not started; otherwise leave CI to 010. For 001 alone: add depth gate step under `jvm-test` job after hollow.

**Out of scope:**
- Changing any Kotlin sources
- Raising thresholds to final 85% DEEP+OK in this plan if current baseline fails — **record baseline** and set CI to fail only on *regression* (worse than baseline) OR fail if script errors; document target thresholds as warnings until plan 012.

## Git workflow

- Branch: `advisor/001-depth-metrics` if git exists
- Commit message style: imperative, e.g. `Add depth_inventory.py tier gate`
- Do NOT push unless operator asks

## Steps

### Step 1: Implement `scripts/depth_inventory.py`

Create a classifier for every `guavakt-*/src/commonMain/kotlin/**/*.kt` file whose stem matches an upstream Guava type (same set as hollow/port_status: under `com/google/common/`).

**Depth tiers (assign exactly one per file):**

| Tier | Criteria (all that apply — use first match from top) |
|------|------------------------------------------------------|
| **SHELL** | File &lt; 30 non-blank lines AND (contains only factories wrapping `LinkedHashMap`/`LinkedHashSet`/`ArrayList` OR only KDoc + pass-through) OR text matches `trimToSize() { /* API preserved */ }` |
| **THIN** | Non-blank lines &lt; 80 OR (extends/implements Guava-shaped type but body is mostly single `delegate` map/list without defining algorithm methods from REQUIRED list in hollow script) |
| **OK** | Non-blank lines ≥ 80 and &lt; 200, or has multiple public methods implementing real logic (no pure delegate for core types) |
| **DEEP** | Non-blank lines ≥ 200 OR listed in allowlist of known-deep stems: `AbstractMapBasedMultimap`, `LocalCache`, `DigestAlgorithms`, `PublicSuffixPatterns`, `TreeRangeSet`, `TreeRangeMap`, `ImmutableList`, `Futures`, `BloomFilter`, `InternetDomainName` (if patterns data large), `Murmur3_128HashFunction`, etc. |

Also flag **ORDERING_DIVERGENCE** (informational, not tier) if file is named `Tree*` / `newTreeMap` / `newTreeSet` and body uses `LinkedHashMap`/`LinkedHashSet` without a custom sorted map implementation (`ComparatorTreeMap` with sorted `entries` counts as OK for ordering if entries are sorted — today they are not fully sorted).

Emit:
```
TOTAL_FILES=N
BY_TIER: DEEP=x OK=y THIN=z SHELL=w
PCT_OK_OR_DEEP=p%
ORDERING_DIVERGENCE=k
BASELINE_PCT_OK_OR_DEEP=<from file or env>
```

Write `build/depth-summary.txt` with same content.

**Exit codes:**
- `0` if script runs and `PCT_OK_OR_DEEP` ≥ `BASELINE` (default baseline: write current value into `scripts/depth_baseline.txt` on first run)
- `1` if PCT drops below baseline or parse errors

First run: if `scripts/depth_baseline.txt` missing, create it with current `PCT_OK_OR_DEEP` (integer percent) and exit 0.

Target for plans README (not enforced until 012): `PCT_OK_OR_DEEP >= 85`.

**Verify**: `python3 scripts/depth_inventory.py` → exit 0; `cat build/depth-summary.txt` shows tiers summing to TOTAL_FILES; `cat scripts/depth_baseline.txt` exists with an integer.

### Step 2: Wire `port_status.py`

At end of `port_status.py`, print one line: `DEPTH_SEE=scripts/depth_inventory.py` or optionally invoke depth and print PCT. Do not break existing `files=... likely_thin=...` line format.

**Verify**: `python3 scripts/port_status.py | tail -5` still shows counts; no exception.

### Step 3: Document in PARITY.md

Add section after hollow summary:

```markdown
## Depth tiers
See `python3 scripts/depth_inventory.py` and `build/depth-summary.txt`.
Tiers: DEEP / OK / THIN / SHELL. CI fails if PCT_OK_OR_DEEP regresses below `scripts/depth_baseline.txt`.
Target for fidelity 9/10: PCT_OK_OR_DEEP ≥ 85 (raised by plans 004–012).
```

**Verify**: `grep -n "Depth tiers" PARITY.md` returns a match.

### Step 4: Add CI step (if `.github/workflows/ci.yml` present)

After hollow inventory step:
```yaml
      - name: Depth inventory gate
        run: python3 scripts/depth_inventory.py
```

**Verify**: YAML still valid; `python3 scripts/depth_inventory.py` exit 0 locally.

### Step 5: Update plans/README.md row 001 → DONE

## Test plan

- No Kotlin tests.
- Manual: delete `scripts/depth_baseline.txt`, run script once (creates baseline), run again (pass), temporarily lower baseline in file to 99 if current &lt; 99 to force fail, confirm exit 1, restore baseline.

## Done criteria

- [ ] `scripts/depth_inventory.py` exists and exits 0 on clean tree
- [ ] `scripts/depth_baseline.txt` committed with initial PCT
- [ ] `build/depth-summary.txt` produced when script runs
- [ ] `PARITY.md` documents depth tiers
- [ ] CI includes depth gate (or explicit note deferred to 010 with script still present)
- [ ] `python3 scripts/hollow_inventory.py` still TOTAL_HOLLOW=0
- [ ] `plans/README.md` status DONE

## STOP conditions

- Cannot find 600+ commonMain kotlin files under guavakt-*
- Hollow inventory starts failing because of unrelated changes — fix hollow first or STOP
- Baseline PCT is 0 or nonsense — fix classifier, do not commit broken baseline

## Maintenance notes

- Raising `depth_baseline.txt` should only happen when PCT improves and maintainers agree (plans 004–012).
- Reviewers: ensure SHELL detection does not flag legitimate small interfaces (e.g. `Funnel.kt` functional interface may be THIN not SHELL if it is only an interface — treat `interface ` only files as OK if they declare methods).
