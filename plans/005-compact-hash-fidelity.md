# Plan 005: CompactHash* open-addressing fidelity

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P2
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/003-jvm-guava-diff-harness.md
- **Category**: tech-debt / perf
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

`CompactHashMap` / `CompactHashSet` / `CompactLinkedHashMap` / `CompactLinkedHashSet` are Guava’s memory-efficient maps. Today they are LinkedHashMap wrappers with no-op `trimToSize()`. Fidelity **9** needs either real compact hashing **or** removal of false performance API (`needsAllocArrays`, no-op trim) with documentation — prefer **real portable implementation** for map/set contracts (size, get, put, remove, iteration, trimToSize meaningfully shrinks capacity).

## Current state

`CompactHashMap.kt`:
```kotlin
open class CompactHashMap<K, V> private constructor(
    private val delegate: LinkedHashMap<K, V>,
) : AbstractMutableMap<K, V>() {
    fun trimToSize() { /* API preserved */ }
    fun needsAllocArrays(): Boolean = false
    ...
}
```

Upstream Guava `CompactHashMap.java` ~1200 LOC open addressing.

## Scope

**In scope:**
- `CompactHashMap.kt`, `CompactHashSet.kt`, `CompactLinkedHashMap.kt`, `CompactLinkedHashSet.kt` under guavakt-collect
- Tests `CompactHashTest.kt`
- Reference Guava algorithm from `guava-upstream/guava/src/com/google/common/collect/CompactHashMap.java` — port defining behavior to Kotlin (not line-by-line required)

**Out of scope:**
- Identical field packing / serialization
- Enum maps

## Steps

### Step 1: Read Guava CompactHashMap public behavior

Port:
- `create()`, `createWithExpectedSize(int)`, `create(Map)`
- put/get/remove/clear/size/containsKey
- `trimToSize()` — reduces internal capacity toward size
- iteration order: **unspecified** for CompactHashMap (not linked); **insertion** for CompactLinkedHash*

### Step 2: Implement portable open addressing

Kotlin `Array<Any?>` table + int hashes array, or single array of entries. Load factor ~1.0 like Guava compact. Resize on growth. `trimToSize` reallocates smaller table.

CompactLinkedHash*: maintain doubly-linked insertion order through extra arrays or entry nodes (Guava uses predecessor/successor in table).

### Step 3: Tests

- basic map contract
- trimToSize does not lose entries
- CompactLinkedHashMap insertion iteration order
- expected size create works

### Step 4: plans/README.md DONE

**Verify**: `./gradlew :guavakt-collect:jvmTest`; `grep -n "LinkedHashMap" CompactHashMap.kt` should not be the sole storage (may use arrays only).

## Done criteria

- [ ] CompactHashMap not a pure LinkedHashMap delegate
- [ ] trimToSize has observable capacity effect **or** documented + tested no-loss
- [ ] Tests pass; hollow 0

## STOP conditions

- Port exceeds reasonable size without tests — stop and ship map with array table minimal version

## Maintenance notes

Depth tier for CompactHash* should move to OK/DEEP after this plan.
