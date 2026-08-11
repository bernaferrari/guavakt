# Stability and support policy

## 0.1.0 contract

`0.1.0` establishes the stable API contract for GuavaKt's **declared Kotlin-first scope**.
The scope is intentionally smaller than Guava: the [compatibility matrix](compatibility.md) and
[Kotlin-first guide](kotlin-first.md) define what is supported and what is deliberately excluded.

Every public API in a published `dev.guavakt:*` module is part of the `0.1.x` compatibility
contract unless its KDoc or the compatibility matrix marks it platform-limited. Private types,
test/parity modules, build scripts, benchmark harnesses, generated data, and `internal` declarations
are not public API.

## Versioning

GuavaKt uses semantic versioning for its published API:

- **Patch releases** (`0.1.x`) preserve source and binary compatibility and contain fixes,
  documentation, dependency, performance, and data updates.
- **Minor releases** add compatible APIs. A public API is deprecated for at least one minor release
  before removal, except to correct a security issue or an implementation that could never honor
  its documented contract.
- **Breaking changes** require the next minor line and a changelog/migration note. The pre-release
  removal of Java-compatibility shims is recorded in `0.1.0` and will not be repeated silently in a
  patch release.

Saved JVM and KLib API descriptions are checked in CI. Compatibility is behavioral only within the
documented scope; GuavaKt never promises binary compatibility with `com.google.guava:guava`.

## Platform support tiers

| Tier | Targets | Commitment |
|---|---|---|
| Tested | JVM, JS Node, Wasm Node | Full test suites run in CI; JVM also runs direct Guava/JDK oracle tests |
| Compile-checked | Linux x64, iOS x64, iOS Simulator Arm64, iOS Arm64, macOS x64/Arm64, MinGW x64 | APIs compile and link as part of KMP/publication verification; common implementations avoid platform-specific runtime requirements |

Availability of platform facilities such as weak references and system filesystems is specified in
the compatibility matrix rather than inferred from a target name.

## Support and maintenance

- The latest published minor line receives bug and security fixes.
- Reproducible correctness, data-loss, security, and cross-target failures are highest priority.
- Performance work requires a reproducible benchmark or trace; throughput from one machine is not a
  compatibility guarantee.
- Public reports should include the module/version, target, Kotlin version, and a minimal
  reproduction. Security reports follow the [security policy](../SECURITY.md).

## Release approval

A release requires green JVM/oracle, API, JS, Wasm, and cross-compilation checks for declared KMP
targets; structural and immutable audits; Dokka; a local publication inspection; and reviewed Maven Central staging. The
maintainer must verify the release POM metadata, signature, source JARs, and generated module
metadata before publishing.
