# Agent / contributor guide — GuavaKt

## Product rules

1. **Kotlin-first**: prefer stdlib for List/Set/Map/immutability/nullability. GuavaKt owns Multimap, Range, Graph, Cache, Hash, Futures-shaped APIs, etc. See `KOTLIN_FIRST.md`.
2. **commonMain by default**; `expect`/`actual` only for GC refs, FS, Proxy, real timers.
3. **Immutable* types must not mutate** — read-only Kotlin interfaces or UOE on mutators. Never `AbstractMutable*` with live `put`.
4. **Honest tiers**: document platform limits (weak refs, FS, reflect) in KDoc and README matrix.

## Verify before claiming done

```bash
./gradlew jvmTest :guavakt-parity:test --no-daemon
python3 scripts/hollow_inventory.py   # TOTAL_HOLLOW=0
python3 scripts/depth_inventory.py    # PCT_OK_OR_DEEP=100%, no wrong-kind
python3 scripts/count_tests.py        # meets MIN_TESTS
```

## Modules

See `DESIGN.md` dependency DAG. Umbrella artifact: `dev.guavakt:guavakt`.

## Do not

- Claim binary compatibility with `com.google.guava:guava`
- Replace kotlinx.coroutines with Guava concurrency as primary model
- Add hollow/bag templates or `TODO: implement` in production sources
