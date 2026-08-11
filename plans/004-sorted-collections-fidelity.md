# Plan 004: Sorted collections fidelity (TreeMap/TreeSet semantics)

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/003-jvm-guava-diff-harness.md (tests first preferred)
- **Category**: bug / tech-debt
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

`TreeMultimap`, `Maps.newTreeMap`, `Sets.newTreeSet`, and parts of `TreeBasedTable` claim sorted semantics but use `LinkedHashMap` / `LinkedHashSet` (insertion order). That is a **fidelity bug**, not a platform limit — sorted order can be implemented in pure Kotlin on commonMain (see existing partial `ComparatorTreeMap` in `TreeBasedTable.kt` that still does not sort `entries`). Reaching fidelity **9** requires real ordering contracts.

## Current state

`TreeMultimap.kt` (excerpt):
```kotlin
private fun <K : Comparable<K>, V> linkedMapFallback(): MutableMap<K, MutableCollection<V>> =
    LinkedHashMap()
// values:
LinkedHashSet()
```

`Maps.kt`:
```kotlin
fun <K : Comparable<K>, V> newTreeMap(): MutableMap<K, V> = LinkedHashMap()
```

`Sets.kt`:
```kotlin
fun <E : Comparable<E>> newTreeSet(): MutableSet<E> = LinkedHashSet() // KMP: no TreeSet
```

`TreeBasedTable.kt` has `internal class ComparatorTreeMap` but:
```kotlin
override val entries: MutableSet<...> get() = data.entries  // LinkedHashMap order, NOT sorted
fun sortedKeys(): List<K> = data.keys.sortedWith { ... }   // only explicit sort helper
```

`TreeMultiset.kt` claims sorted order exposure — verify and fix if iteration not sorted.

Exemplar for multimap structure: `AbstractSetMultimap` / `AbstractMapBasedMultimap` in collect module.

## Commands

| Purpose | Command | Expected |
|---------|---------|----------|
| Tests | `./gradlew :guavakt-collect:jvmTest --no-daemon` | SUCCESS |
| Hollow | `python3 scripts/hollow_inventory.py` | TOTAL_HOLLOW=0 |
| Depth | `python3 scripts/depth_inventory.py` | exit 0 (may improve PCT) |

## Scope

**In scope:**
- `guavakt-collect/src/commonMain/kotlin/com/google/common/collect/ComparatorTreeMap.kt` (extract/promote from TreeBasedTable or replace)
- `guavakt-collect/.../ComparatorTreeSet.kt` (new) — `MutableSet` with sorted iteration
- `TreeMultimap.kt`, `Maps.kt`, `Sets.kt`, `TreeBasedTable.kt`, `TreeMultiset.kt` as needed
- `ElementOrder.kt` if NATURAL uses unsorted LinkedHashMap for graphs — fix to sorted iteration if Guava requires
- Tests: `guavakt-collect/src/commonTest/.../SortedCollectionsTest.kt` (new)
- Parity tests in guavakt-parity if module exists

**Out of scope:**
- Red-black tree performance identical to `java.util.TreeMap` (use sorted list or tree; **must** preserve sort on entrySet/keySet/iterator)
- ConcurrentNavigableMap
- Guava serialization of TreeMap

## Steps

### Step 1: Implement real `ComparatorTreeMap<K,V>` in commonMain

Requirements:
- Constructor `(comparator: Comparator<in K>?)` — null means keys are `Comparable`
- `put` / `get` / `remove` / `containsKey` / `clear` / `size`
- `entries`, `keys`, `values` iteration in **comparator order** (not insertion order)
- Optional: `firstKey` / `lastKey` if easy

Implementation options (pick one; prefer simplicity + correct order):
1. Store `LinkedHashMap` for O(1) lookup + rebuild sorted entry list on mutate (OK for tests; fine for moderate n)
2. Store `ArrayList` of pairs kept sorted with binary search

Must **not** expose insertion order as iteration order.

Move class to its own file; update `TreeBasedTable` to use it; make `entries` sorted.

**Verify**: unit test put keys `c,a,b` → keys iterator `a,b,c`.

### Step 2: Implement `ComparatorTreeSet<E>`

Backed by `ComparatorTreeMap<E, Boolean>` or sorted list. Iteration sorted. Used for TreeMultimap values and `Sets.newTreeSet`.

### Step 3: Wire factories and TreeMultimap

- `Maps.newTreeMap()` → `ComparatorTreeMap(null)`
- `Maps.newTreeMap(comparator)` → `ComparatorTreeMap(comparator)`
- `Maps.newTreeMap(map)` → copy into ComparatorTreeMap
- `Sets.newTreeSet()` / with comparator / from iterable → ComparatorTreeSet
- `TreeMultimap` key map = ComparatorTreeMap; value sets = ComparatorTreeSet

Update KDocs to remove “KMP: LinkedHashMap” divergence notes; say “sorted by natural order / comparator (portable implementation)”.

### Step 4: Fix TreeMultiset element iteration order

Read `TreeMultiset.kt`; ensure `iterator()` / `elementSet()` visit in sorted order.

### Step 5: Tests

`SortedCollectionsTest.kt` model after `CollectParityTest.kt`:
- newTreeMap iteration order
- newTreeSet iteration order  
- TreeMultimap keySet order and values per key order
- TreeBasedTable rowKeySet order
- TreeMultiset element order
- Custom comparator reverse order

### Step 6: Enable any `pending004` parity tests from plan 003

### Step 7: Raise depth baseline if PCT improved (optional, careful)

### Step 8: plans/README.md DONE

## Done criteria

- [ ] No `LinkedHashMap()` as implementation of `newTreeMap` / TreeMultimap keys
- [ ] No `LinkedHashSet()` as implementation of `newTreeSet` / TreeMultimap values
- [ ] SortedCollectionsTest passes
- [ ] `:guavakt-collect:jvmTest` SUCCESS
- [ ] hollow 0
- [ ] README matrix row for Tree* can say “sorted on all targets” (update in 002 if already merged)

## STOP conditions

- AbstractSetMultimap cannot accept custom map/set factories — read AbstractMapBasedMultimap constructors and adapt
- Performance concerns — still ship correct order; optimize later

## Maintenance notes

Reviewers must reject PRs that reintroduce LinkedHash* for Tree* types without tests failing.
