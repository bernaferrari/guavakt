# Plan 012: Collect core depth sweep (thin → OK/DEEP)

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P2
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/004, 005, 009
- **Category**: tech-debt
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

After sorted + compact hash + tests, remaining **THIN/SHELL** stems in collect (~215 files, largest module) dominate depth PCT. Sweep raises `PCT_OK_OR_DEEP` to **≥ 85%** repo-wide (fidelity **9** metric from plan 001) by deepening highest-impact thin types and deleting false APIs.

## Current state

`python3 scripts/port_status.py` historically: `likely_thin=345` of 610. Collect holds many Forwarding*, Immutable* edges, AbstractNavigableMap as LinkedHashMap, Serialization helpers, Interners, etc.

## Scope

**In scope:**
- Any `guavakt-collect/src/commonMain/**/*.kt` classified THIN/SHELL by `depth_inventory.py` that is **userable** without JVM
- Priority deepen list (do in order until PCT ≥ 85 **or** all priority items done):

1. `AbstractNavigableMap` / `ForwardingNavigableMap` / `ForwardingNavigableSet` — navigable ops with sorted keys (use ComparatorTreeMap from 004)
2. `Interners` — strong interner map
3. `ObjectArrays` / `Preparing` utilities if thin
4. `ImmutableEnumMap` / `ImmutableClassToInstanceMap` — real semantics where KClass allows
5. `EnumBiMap` / `EnumHashBiMap`
6. Remaining `Forwarding*` — ensure `delegate()` abstract and all interface methods forward (not empty)
7. `Serialization.kt` — keep KMP populate helpers; add KDoc not claiming Java serialization
8. `Streams` / `MoreCollectors` if present and thin

- Raise `scripts/depth_baseline.txt` to new PCT when ≥ 85
- Tests for each deepened type (min 2 @Test each)

**Out of scope:**
- reflect, concurrent (other plans)
- Guava testlib

## Steps

### Step 1: Run depth inventory; save list of SHELL+THIN in collect

```bash
python3 scripts/depth_inventory.py
# If script lists by file, use that; else extend script to --print-thin
```

If script lacks listing, add `--list-thin` flag outputting paths.

### Step 2: For each priority stem, open Guava upstream twin and port defining public methods

Pattern: read Guava Java public methods → implement in Kotlin using existing module utilities.

### Step 3: After each batch of ~10 files, run `:guavakt-collect:jvmTest` and depth inventory

### Step 4: When `PCT_OK_OR_DEEP >= 85`, write new baseline to `scripts/depth_baseline.txt`

### Step 5: Update PARITY.md “Depth tiers” with achieved PCT

### Step 6: plans/README.md — mark 012 DONE; note overall 9/9/9 if all plans done

**Verify**:
```bash
python3 scripts/depth_inventory.py   # PCT >= 85, exit 0
python3 scripts/count_tests.py       # >= 800
python3 scripts/hollow_inventory.py  # TOTAL_HOLLOW=0
./gradlew jvmTest --no-daemon        # SUCCESS
```

## Done criteria

- [ ] PCT_OK_OR_DEEP ≥ 85
- [ ] depth_baseline.txt updated to ≥ 85
- [ ] Priority stems 1–6 deepened or explicitly deferred in PARITY with reason
- [ ] All gates green

## STOP conditions

- Cannot reach 85% without multi-week effort — deepen priority list fully, set baseline to current, document remaining SHELL list in PARITY.md `## Remaining thin stems`, and mark plan DONE with note **fidelity metric partial** — operator accepts 80%+ only if priority stems done

## Maintenance notes

New Guava stems must land as OK not SHELL. CI depth gate prevents regression.
