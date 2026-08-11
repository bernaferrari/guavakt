# Public Suffix List refresh discipline

`InternetDomainName` uses a checked-in, compact trie in common code. It never fetches data at runtime, which keeps behavior deterministic and works on every Kotlin target. The trie is a data snapshot, not a claim that a release always contains the newest Public Suffix List (PSL).

The sole authoritative source is [https://publicsuffix.org/list/public_suffix_list.dat](https://publicsuffix.org/list/public_suffix_list.dat). The project refresh tool preserves ICANN/registry versus private-rule classification, wildcard rules, and exceptions while regenerating the compact representation used by `PublicSuffixTrie`.

## Routine check

Run this before a net-focused release and at least monthly:

```bash
python3 scripts/refresh_public_suffix.py status
```

It reports the canonical PSL `VERSION`, `COMMIT`, and SHA-256, then compares the complete semantic rule set with the checked-in trie. `check` is suitable for a scheduled network-enabled job and exits nonzero only when a source refresh is necessary:

```bash
python3 scripts/refresh_public_suffix.py check
```

The normal Gradle test suite deliberately does not run this command because an availability issue at the upstream data host must not make a local or offline correctness build fail.

## Reviewed refresh

When `status` says `REFRESH_REQUIRED`, run the generator explicitly. It is intentionally unable to overwrite production source without naming the output file.

```bash
python3 scripts/refresh_public_suffix.py generate \
  --output guavakt-net/src/commonMain/kotlin/com/google/thirdparty/publicsuffix/PublicSuffixPatterns.kt \
  --lock-output public_suffix.lock.json
./gradlew :guavakt-net:jvmTest :guavakt-parity:test --tests dev.guavakt.parity.NetDifferentialTest \
  --no-daemon -Pkotlin.incremental=false
```

Review the generated source, the lock's version/commit/SHA-256, and PSL-sensitive domain examples before accepting the update. The generator round-trips its own output against every official rule before it writes the lock; the follow-up tests protect the public `InternetDomainName` behavior. Keep the generated header and `public_suffix.lock.json` together with any refresh.
