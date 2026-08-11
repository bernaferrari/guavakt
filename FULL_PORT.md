# GuavaKt full port strategy

**Goal:** Guava-shaped APIs and algorithms for Kotlin Multiplatform.  
**Default:** implement in **`commonMain` (pure Kotlin)**.  
**Exception:** **`jvmMain` `actual`** only when the platform must supply GC, disk, threads, or proxies.

## What “full port” means here

| In scope | Out of scope (unless you add later) |
|----------|-------------------------------------|
| Every Guava **main type stem** has a twin in `commonMain` | Bit-identical JVM heap layouts |
| Defining **public algorithms** match Guava contracts / standard vectors | Porting all of `guava-tests` |
| JVM gets Guava-like **weak/soft**, **NIO Files**, **Proxy** via actuals | Pretending weak refs work on JS |
| One behavior for crypto/PSL/collections on all targets | Replacing Guava JAR as Maven coordinate |

## Architecture

```
commonMain  → algorithms + Guava API (Kotlin)
jvmMain     → java.lang.ref, java.nio.file, Proxy, Class.forName, optional schedulers
js/native/wasm → strong refs / explicit unsupported for FS & Proxy
```

## How to work (repeatable loop)

1. **Pick a Guava package** (e.g. `collect`, `util.concurrent`) — not random files.
2. **Inventory** that package: `python3 scripts/port_status.py collect`
3. For each thin type: open Guava `.java`, port **defining public API** into existing `.kt` twin.
4. Prefer **pure Kotlin** in commonMain; only add `expect` if Guava **requires** JVM (GC, NIO, Proxy, ClassLoader).
5. Add **1–3 contract tests** for that type (or package).
6. Run `./gradlew jvmTest` (and `compileKotlinJs` for touched modules).
7. Mark package **done** in the checklist below when tests pass and no bag/shell patterns remain.

## Package checklist (update as you go)

- [x] annotations, base (core preconditions/strings/splitters — ongoing depth)
- [x] primitives (unsigned, arrays)
- [x] math (quantiles, stats, linear; Long limits for BigInteger called out)
- [x] hash (Murmur, SipHash, CRC, MD5/SHA/HMAC pure Kotlin, FarmHash-ish)
- [x] net PSL + InternetDomainName (full Guava trie data)
- [x] cache LocalCache + weak/soft/weakKeys + CacheBuilderSpec (JVM refs)
- [x] collect MapMaker strengths, tables, multimaps (depth varies by type)
- [x] collect Immutable* wrong-kind drain (read-only contracts + audit gate)
- [x] concurrent: AbstractFuture/SettableFuture/Futures composition depth (timed waits still cooperative)
- [ ] io: common streams done; JVM Files actual done; MoreFiles NIO sweep on jvmMain
- [x] reflect: TypeToken hierarchy + ImmutableTypeToInstanceMap contracts (Proxy still platform-limited)
- [ ] graph: networks done; minor builders polish
- [ ] eventbus: core done; async polish optional

## Commands

```bash
# Status by module keyword
python3 scripts/port_status.py

# Hollow / wrong-kind gate
python3 scripts/hollow_inventory.py

# Tests
./gradlew jvmTest
./gradlew :guavakt-hash:compileKotlinJs   # when you touch portable code
```

## When to use JVM actual (allowed list)

1. WeakReference / SoftReference / ReferenceQueue  
2. java.nio.file.Files (read/write/temp)  
3. java.lang.reflect.Proxy / Class.forName  
4. ClassLoader scanning for ClassPath (optional next)  
5. ScheduledExecutorService for real TimeoutFuture (optional next)

Everything else → **commonMain Kotlin**.

## Decision for the animal shelter app

- Deploy **JVM** → use GuavaKt as primary; weak caches + Files + Proxy work via actuals.  
- Shared **KMP client** later → same APIs; weak/FS/proxy limited on non-JVM (documented).  
- Do **not** block the app on 100% type depth; extend GuavaKt when the app imports a thin type.
