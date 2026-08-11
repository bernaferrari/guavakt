# Releasing GuavaKt

The Gradle build can create signed Kotlin Multiplatform publications for the Maven Central Portal. Publishing is intentionally not automatic. The public source repository is [bernaferrari/guavakt](https://github.com/bernaferrari/guavakt); publication metadata must name that repository before the first upload.

## One-time owner setup

1. Replace the temporary `https://github.com/guavakt/guavakt` POM/SCM URLs in `build.gradle.kts` with `https://github.com/bernaferrari/guavakt`.
2. Verify ownership of the `dev.guavakt` namespace in the Maven Central Portal before publishing under that coordinate.
3. Generate a Central user token and an OpenPGP signing key.
4. Store credentials only in the release environment as Gradle properties or environment-backed Gradle properties; never commit secrets.

Expected secret properties are `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`, and `signingInMemoryKeyPassword`.

## Release checks

Change the root version from `0.1.0-SNAPSHOT` to the intended release, then run:

```bash
./gradlew jvmTest :guavakt-parity:test apiCheck dokkaGenerate --no-daemon
python3 scripts/hollow_inventory.py
python3 scripts/depth_inventory.py
python3 scripts/count_tests.py
python3 scripts/immutable_audit.py
./gradlew publishToMavenLocal --no-daemon
```

Inspect the local POMs, module metadata, sources, documentation artifacts, and API dump diff. After a clean release commit/tag, upload without automatic publication:

```bash
./gradlew publishAllPublicationsToMavenCentralRepository \
  -PsignAllPublications=true --no-daemon
```

Review and publish the validated deployment in the Central Portal. Do not claim the artifacts are available until Central search resolves them.
