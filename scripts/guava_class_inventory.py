#!/usr/bin/env python3
"""Compare GuavaKt's top-level types with public Guava source types.

This is a surface inventory, not parity evidence. Behavioral compatibility still
requires typed differential tests as described in docs/compatibility.md.
"""

from __future__ import annotations

import argparse
import re
import subprocess
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_UPSTREAM = ROOT / "guava-upstream" / "guava" / "src" / "com" / "google" / "common"
JAVA_PACKAGE = re.compile(r"(?m)^package\s+([\w.]+);")
KOTLIN_PACKAGE = re.compile(r"(?m)^package\s+([\w.]+)")


def public_guava_types(source_root: Path) -> set[str]:
    result: set[str] = set()
    for path in source_root.rglob("*.java"):
        if path.name in {"package-info.java", "module-info.java"}:
            continue
        text = path.read_text(errors="replace")
        name = path.stem
        declaration = re.compile(
            rf"(?m)^public\s+(?:(?:abstract|final|sealed|non-sealed|static)\s+)*"
            rf"(?:class|interface|enum|@interface|record)\s+{re.escape(name)}\b"
        )
        package = JAVA_PACKAGE.search(text)
        if package and declaration.search(text):
            result.add(f"{package.group(1)}.{name}")
    return result


def guavakt_types() -> set[str]:
    result: set[str] = set()
    declaration = re.compile(
        r"(?m)^(?:(?:public|internal|expect|actual|abstract|open|sealed|data|value|enum|annotation|fun)\s+)*"
        r"(?:class|interface|object|typealias)\s+([A-Za-z_]\w*)\b"
    )
    for module in sorted(ROOT.glob("guavakt*")):
        source = module / "src"
        if not source.is_dir():
            continue
        for path in source.glob("*Main/kotlin/**/*.kt"):
            text = path.read_text(errors="replace")
            package = KOTLIN_PACKAGE.search(text)
            if not package:
                continue
            for name in declaration.findall(text):
                result.add(f"{package.group(1)}.{name}")
    return result


def oracle_revision() -> str:
    try:
        return subprocess.run(
            ["git", "-C", str(ROOT / "guava-upstream"), "rev-parse", "--short", "HEAD"],
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream", type=Path, default=DEFAULT_UPSTREAM)
    parser.add_argument("--fail-on-missing", action="store_true")
    args = parser.parse_args()

    if not args.upstream.is_dir():
        parser.error(f"Guava source root does not exist: {args.upstream}")

    guava = public_guava_types(args.upstream)
    guavakt = guavakt_types()
    expected = {name.replace("com.google.common.", "com.bernaferrari.guavakt.", 1) for name in guava}
    missing = sorted(expected - guavakt)
    covered = expected & guavakt
    by_package = Counter(name.rsplit(".", 1)[0] for name in missing)

    print(f"ORACLE_REVISION={oracle_revision()}")
    print(f"GUAVA_PUBLIC_TOP_LEVEL_TYPES={len(expected)}")
    print(f"GUAVAKT_PUBLIC_NAME_COVERAGE={len(covered)}")
    print(f"MISSING_PUBLIC_NAMES={len(missing)}")
    for package, count in sorted(by_package.items()):
        print(f"MISSING_PACKAGE {package}={count}")
    for name in missing:
        print(f"MISSING {name}")
    print("NOTE=Name coverage is inventory only; it does not establish behavioral parity")
    return 1 if args.fail_on_missing and missing else 0


if __name__ == "__main__":
    raise SystemExit(main())
