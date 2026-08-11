# Plan 008: Reflect tiering + TypeToken depth on JVM

> **Executor instructions**: Follow step by step. STOP on drift. Update `plans/README.md` when done.

## Status

- **Priority**: P2
- **Effort**: L
- **Risk**: MED
- **Depends on**: plans/003-jvm-guava-diff-harness.md
- **Category**: tech-debt / migration
- **Planned at**: workspace snapshot 2026-06-28

## Why this matters

`TypeToken` is a 13-line `KClass` wrapper vs Guava ~1476 LOC. Reflect cannot be fully faithful on JS/Native, but architecture **9** needs **honest tiering** (common API minimal + JVM deepen) so consumers know where fidelity lives. Fidelity **9** overall requires JVM TypeToken to handle `getRawType`, `resolveType`, subtype checks beyond equality for common cases (`List<String>` vs `List` — limited on KMP without reified), and ClassPath registry vs JVM scan.

## Current state

```kotlin
// TypeToken.kt commonMain
open class TypeToken<T : Any> private constructor(private val raw: kotlin.reflect.KClass<T>) {
    open fun getRawType(): kotlin.reflect.KClass<T> = raw
    open fun isSubtypeOf(type: TypeToken<*>): Boolean = raw == type.raw
    ...
}
```

`PlatformReflection` jvmMain has Proxy; js/native/wasm throw UOE.

`ClassPath` is registry-based on common.

## Scope

**In scope:**
- `guavakt-reflect` commonMain TypeToken, TypeResolver, ClassPath KDocs
- `guavakt-reflect/src/jvmMain` deepen TypeToken with `java.lang.reflect.Type` where useful via expect/actual optional
- Tests jvmTest for TypeToken of classes and isSubtypeOf for class hierarchy (`Number` vs `Integer` on JVM using KClass hierarchy: `raw.isInstance` / supertypes)
- README / PARITY reflect row

**Out of scope:**
- Full Guava TypeResolver capture conversion
- Guaranteed parity for all wildcards on non-JVM

## Steps

### Step 1: Document tier in every reflect public type KDoc

Prefix: `/** Guava TypeToken — commonMain: KClass erasure only. JVM: see jvmMain extensions / deeper isSubtypeOf. */`

### Step 2: Improve common TypeToken

- `isSubtypeOf`: use `raw.isSubclassOf` / Kotlin reflection `isSubclassOf` from `kotlin.reflect.full` — **may be JVM-only**. If `kotlin.reflect.full` not on common, implement:
  - common: keep equality
  - jvmMain: `actual` extended TypeToken **or** expect fun `isSubtypeOfPlatform(a: KClass<*>, b: KClass<*>): Boolean`

Use expect/actual:
```kotlin
internal expect fun kClassIsSubtypeOf(sub: KClass<*>, sup: KClass<*>): Boolean
// jvm: sub.java.isAssignableFrom checks reversed — sub assignable to sup
// js: sub == sup
```

### Step 3: ClassPath on JVM

Optional: jvmMain actual scans classpath if low effort; else KDoc “registry only on all targets” and ensure `ClassPath.from` registry API works.

### Step 4: Tests

`TypeTokenTest` in commonTest/jvmTest — Number/Integer on JVM.

### Step 5: plans/README.md DONE

## Done criteria

- [ ] Reflect limitations documented in README matrix (plan 002 update if needed)
- [ ] JVM isSubtypeOf works for simple class hierarchies
- [ ] Proxy still UOE off JVM
- [ ] hollow 0; reflect tests pass

## STOP conditions

- kotlin.reflect.full not available — use Java Class on jvmMain only via expect

## Maintenance notes

Do not pretend commonMain has Java Type.
