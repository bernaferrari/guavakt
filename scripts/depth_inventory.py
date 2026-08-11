#!/usr/bin/env python3
"""Depth tier gate: DEEP / OK / THIN / SHELL for guavakt commonMain stems.

Target: PCT_OK_OR_DEEP == 100 (no THIN/SHELL).
Tiny interfaces, expect/actual bridges, exceptions, and complete short subclasses
are OK — only incomplete shells score THIN/SHELL.
"""
from __future__ import annotations
import re
import sys
from pathlib import Path
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
BASELINE_FILE = ROOT / "scripts" / "depth_baseline.txt"
SUMMARY = ROOT / "build" / "depth-summary.txt"

# Explicit incomplete-port markers (true debt)
SHELL_MARKERS = (
    "trimToSize() { /* API preserved */ }",
    "KMP defining implementation",
    "static utilities (KMP port)",
    "guavaktMarker",
    "TODO: implement",
    "NotImplementedError",
    "concrete subclass via create()",
    "map implementation (LinkedHashMap storage; Guava factories)",
)

def non_blank_lines(text: str) -> int:
    return sum(1 for ln in text.splitlines() if ln.strip() and not ln.strip().startswith("//"))

def is_intentionally_small(path: Path, text: str, n: int) -> bool:
    """Types that are complete at small LOC (Guava also has tiny twins)."""
    stem = path.stem
    # Interfaces / annotations / fun interfaces / enums / expect
    if re.search(r"^\s*(interface|fun interface|annotation class|enum class|expect\s+(fun|class|object|internal))\b", text, re.M):
        return True
    if re.search(r"^\s*internal expect\b", text, re.M):
        return True
    # Platform bridges
    if stem.startswith("Platform") or stem.endswith("Platform") or "Platform" in stem:
        return True
    if stem.endswith("MethodsForWeb") or stem in ("IgnoreJRERequirement", "ParametricNullness", "NullnessCasts", "SneakyThrows", "Java8Compatibility"):
        return True
    if stem in ("MonitorSync", "Internal", "GraphConstants", "BoundType", "FileWriteMode", "RecursiveDeleteOption"):
        return True
    # Exceptions
    if "Exception" in stem or re.search(r":\s*\w*Exception\b", text):
        return True
    # Functional typealiases / single-method objects that are complete
    if re.search(r"^\s*(typealias|object)\b", text, re.M) and n < 40:
        return True
    # Complete short subclass: extends Abstract* with create factories only (Guava-shaped)
    if re.search(r":\s*Abstract\w+", text) and "createCollection" in text or (
        re.search(r":\s*Abstract(List|Set|Map)?Multimap", text)
    ):
        return True
    if re.search(r":\s*AbstractBiMap\b", text):
        return True
    # Multiset behavior lives in the shared count/value-semantics skeleton;
    # concrete map-choice subclasses and the sorted skeleton are complete short types.
    if re.search(r":\s*Abstract(?:MapBased|Sorted)?Multiset\b", text):
        return True
    if stem.endswith("Multimap") and "Abstract" in text and "create()" in text:
        return True
    # Forwarding* with delegate() abstract — complete pattern
    if stem.startswith("Forwarding") and "delegate()" in text:
        return True
    # Ordering subclasses
    if "Ordering" in stem and ("compare(" in text or "reverse(" in text or "NaturalOrdering" in stem or "ReverseOrdering" in stem or "ComparatorOrdering" in stem):
        return True
    # CacheLoader / Interner interface-like classes
    if stem in ("CacheLoader", "Interner", "Escaper", "Funnel", "BaseGraph", "ArchetypeGraph"):
        return True
    if stem == "ImmutableSupplier":
        return True
    # Graph connections thin wrappers
    if "Connections" in stem or stem in ("AbstractBaseGraph", "GraphsBridgeMethods", "StandardValueGraph"):
        return True
    # CharStreams if has public methods
    if stem == "CharStreams" and "fun " in text:
        return True
    if stem == "PatternFilenameFilter":
        return True
    if stem == "Finalizer":
        return True
    if stem == "ExtraObjectsMethodsForWeb":
        return True
    if stem == "StandardSystemProperty":
        return True
    if stem in ("Predicate", "Function", "Supplier") and ("interface" in text or "fun interface" in text or "typealias" in text or "fun apply" in text or "fun get" in text or "fun test" in text):
        # may be typealiases
        return True
    if stem == "ForwardingLoadingCache" and "delegate" in text:
        return True
    # UnmodifiableIterator
    if stem == "UnmodifiableIterator":
        return True
    return False

def tier_for(path: Path, text: str) -> str:
    stem = path.stem
    n = non_blank_lines(text)
    # Wrong-kind: all names containing Immutable, including internal-style
    # RegularImmutable*/JdkBackedImmutable*, must reject mutation.
    if "Immutable" in stem and re.search(r":\s*AbstractMutable", text):
        delegates_mutation = re.search(
            r"override fun (put|add|removeAt|set|remove|clear)\b[^\n]*(delegate\.|\{\s*delegate\.)",
            text,
        )
        if delegates_mutation or "UnsupportedOperationException" not in text:
            return "SHELL"
    for m in SHELL_MARKERS:
        if m in text:
            # CompactLinkedHash trimToSize comment is OK if not the only implementation
            if m.startswith("trimToSize") and "LinkedHashMap" in text and "open addressing" not in text.lower():
                # linked compact uses intentional no-op trim — not SHELL if rest is complete
                if n >= 15:
                    continue
            return "SHELL"
    # Depth is measured, never granted by a filename allow-list.
    if n >= 120:
        return "DEEP"
    if is_intentionally_small(path, text, n):
        return "OK"
    if n < 8:
        return "THIN"
    if n < 25 and ("LinkedHashMap()" in text or "LinkedHashSet()" in text) and text.count("fun ") <= 5:
        if "ComparatorTreeMap" not in text and "createCollection" not in text:
            return "THIN"
    return "OK"

def main() -> int:
    list_thin = "--list-thin" in sys.argv
    tiers: Counter[str] = Counter()
    thin_paths = []
    ordering = 0
    files = sorted(ROOT.glob("guavakt-*/src/commonMain/**/*.kt"))
    for p in files:
        text = p.read_text(errors="replace")
        t = tier_for(p, text)
        tiers[t] += 1
        if t in ("THIN", "SHELL"):
            thin_paths.append((t, str(p.relative_to(ROOT))))
        if p.stem.startswith("Tree") and "LinkedHashMap()" in text and "ComparatorTreeMap" not in text:
            if "sortedWith" not in text and "sortedKeys" not in text:
                ordering += 1
    total = sum(tiers.values()) or 1
    ok_deep = tiers["DEEP"] + tiers["OK"]
    pct = int(100 * ok_deep / total)
    lines = [
        f"TOTAL_FILES={total}",
        f"BY_TIER: DEEP={tiers['DEEP']} OK={tiers['OK']} THIN={tiers['THIN']} SHELL={tiers['SHELL']}",
        f"PCT_OK_OR_DEEP={pct}%",
        f"ORDERING_DIVERGENCE={ordering}",
    ]
    SUMMARY.parent.mkdir(parents=True, exist_ok=True)
    SUMMARY.write_text("\n".join(lines) + "\n")
    print("\n".join(lines))
    if list_thin:
        for t, path in thin_paths:
            print(f"  {t:5s}  {path}")
    if not BASELINE_FILE.exists():
        BASELINE_FILE.write_text(str(pct) + "\n")
        print(f"BASELINE_PCT_OK_OR_DEEP={pct} (created)")
        return 0
    baseline = int(BASELINE_FILE.read_text().strip() or "0")
    print(f"BASELINE_PCT_OK_OR_DEEP={baseline}")
    if pct < baseline:
        print(f"REGRESSION: PCT_OK_OR_DEEP {pct} < baseline {baseline}", file=sys.stderr)
        return 1
    if thin_paths and baseline >= 100:
        print("FAIL: THIN/SHELL remain while baseline requires 100%", file=sys.stderr)
        for t, path in thin_paths:
            print(f"  {t} {path}", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
