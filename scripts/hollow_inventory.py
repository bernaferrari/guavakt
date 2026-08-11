#!/usr/bin/env python3
"""Structural hollow gate: wrong super-type family + missing defining members."""
from __future__ import annotations
import re
import sys
from pathlib import Path
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]

# Stem name pattern -> forbidden super/interface patterns (regex on file text)
WRONG_SUPER = [
    # Multimap must not be List forwarding
    (re.compile(r"Multimap"), re.compile(r"ForwardingList<|:\s*ForwardingList\b|AbstractMutableList")),
    # Network / Connections / *GraphBuilder / AbstractGraphBuilder must not be MutableGraph-by-delegate only shells
    (re.compile(r"(Network|GraphConnections|Connections|NetworkBuilder|ValueGraphBuilder|AbstractGraphBuilder)$"),
     re.compile(r"MutableGraph\w*\s+by\s+delegate")),
    # GraphBuilder is valid if it has directed/undirected/build — only flag by-delegate shells
    (re.compile(r"GraphBuilder$"),
     re.compile(r"MutableGraph\w*\s+by\s+delegate")),
    # Named Iterator must not be AbstractMutableList
    (re.compile(r"Iterator"), re.compile(r"AbstractMutableList|:\s*AbstractMutableList")),
]

# Repeated generator bodies that have appeared under unrelated Guava type names.
# These are wrong-kind implementations, not merely shallow implementations.
TEMPLATE_MARKERS = (
    "concrete subclass via create()",
    "map implementation (LinkedHashMap storage; Guava factories)",
)

# Per-stem required defining API fragments (substring must appear in twin)
REQUIRED_MEMBERS: dict[str, list[str]] = {
    "ForwardingListMultimap": ["ListMultimap", "delegate()", "get(", "put(", "removeAll"],
    "TransformedIterator": ["transform(", "Iterator", "hasNext", "next"],
    "AbstractSequentialIterator": ["computeNext", "Iterator", "hasNext"],
    "AbstractNetwork": ["Network", "nodes()", "edges()", "incidentEdges"],
    "StandardNetwork": ["Network", "nodes()", "edges()"],
    "ImmutableNetwork": ["Network", "nodes()", "edges()"],
    "ForwardingNetwork": ["Network", "delegate()"],
    "GraphConnections": ["adjacentNode", "incidentEdges", "successors"],
    "ValueGraphBuilder": ["directed", "undirected", "build"],
    "AbstractGraphBuilder": ["directed", "allowsSelfLoops"],
    "NetworkBuilder": ["directed", "undirected", "build"],
}

# Also scan for mass shell pattern on Network/Graph families
SHELL_BY_DELEGATE = re.compile(
    r"class\s+(\w+).*MutableGraph<\w+>\s+by\s+delegate",
    re.S,
)

def classify_file(path: Path, text: str) -> list[str]:
    reasons: list[str] = []
    stem = path.stem
    declaration = re.search(
        rf"\b(?:class|interface|object)\s+{re.escape(stem)}\b.*?\{{",
        text,
        re.S,
    )
    # Wrong-kind checks apply to the file's defining declaration, not legitimate
    # nested view implementations such as a Multimap's AbstractMutableList wrapper.
    defining_header = declaration.group(0) if declaration else text[:1000]
    for marker in TEMPLATE_MARKERS:
        if marker in text:
            reasons.append("generated_wrong_kind_template")
            break

    # Every type whose name promises immutability must reject mutation. Guava's
    # internal names frequently prefix Immutable with Regular/JdkBacked/etc.
    if "Immutable" in stem and re.search(r":\s*AbstractMutable(Map|Set|List|Collection)\b", text):
        if re.search(r"override fun (put|add|removeAt|set|remove|clear)\b[^\n]*(delegate\.|\{\s*delegate\.)", text):
            reasons.append("immutable_wrong_kind_mutable")
        elif "throw UnsupportedOperationException" not in text:
            reasons.append("immutable_wrong_kind_mutable")
    for stem_pat, bad_super in WRONG_SUPER:
        if stem_pat.search(stem) and bad_super.search(defining_header):
            # Network implements Network is OK; only flag if MutableGraph by delegate without Network
            if "Network" in stem and "interface Network" in text:
                continue
            if "Network" in stem and re.search(r":\s*Network\b|:\s*MutableNetwork\b", text) and "by delegate" not in text:
                continue
            if "Builder" in stem and "fun build" in text and "MutableGraph by delegate" not in text:
                # builders that build() are ok even if named GraphBuilder
                if "by delegate" not in text:
                    continue
            reasons.append(f"wrong_supertype:{stem_pat.pattern}")
    if stem in REQUIRED_MEMBERS:
        for frag in REQUIRED_MEMBERS[stem]:
            if frag not in text:
                reasons.append(f"missing_member:{frag}")
    # Graph family shell (any stem using MutableGraph by delegate is wrong-kind mass shell)
    m = SHELL_BY_DELEGATE.search(text)
    if m and m.group(1) == stem:
        reasons.append("graph_shell_mutablegraph_by_delegate")
    # Drop false positives: proper interface implementors.
    if "ListMultimap" in text and "removeAll" in text and "put(" in text:
        reasons = [r for r in reasons if "Multimap" not in r or "missing_member" in r or "wrong_supertype" not in r]

    # Mass hash delegate to murmur3_32 (wrong algorithm for 128-bit / SipHash / MAC / digest)
    if "Hashing.murmur3_32()" in text and path.stem not in ("Murmur3_32HashFunction", "Hashing", "HashTest"):
        if any(x in path.stem for x in ("Murmur3_128", "SipHash", "MessageDigest", "MacHash", "ChecksumHash", "Abstract")):
            reasons.append("murmur3_32_mass_delegate")
    return reasons  # drop false

def main(out_dir: Path | None = None) -> int:
    out_dir = out_dir or (ROOT / "build")
    out_dir.mkdir(parents=True, exist_ok=True)
    hits: list[tuple[str, str]] = []
    by_reason: Counter[str] = Counter()
    by_module: Counter[str] = Counter()
    # Priority exemplars first — always evaluate
    priority = list(REQUIRED_MEMBERS.keys())
    seen = set()
    for stem in priority:
        paths = list(ROOT.glob(f"guavakt-*/src/commonMain/**/{stem}.kt"))
        for p in paths:
            text = p.read_text(errors="replace")
            rel = str(p.relative_to(ROOT))
            for r in classify_file(p, text):
                hits.append((rel, r))
                by_reason[r] += 1
                by_module[rel.split("/")[0]] += 1
            seen.add(rel)
    # Full scan for wrong super / shells
    for p in sorted(ROOT.glob("guavakt-*/src/commonMain/**/*.kt")):
        rel = str(p.relative_to(ROOT))
        if rel in seen:
            continue
        text = p.read_text(errors="replace")
        for r in classify_file(p, text):
            hits.append((rel, r))
            by_reason[r] += 1
            by_module[rel.split("/")[0]] += 1
    stems_path = out_dir / "hollow-stems.txt"
    stems_path.write_text("\n".join(f"{reason}\t{path}" for path, reason in hits) + ("\n" if hits else ""))
    summary = out_dir / "hollow-summary.txt"
    lines = [f"TOTAL_HOLLOW={len(hits)}", "BY_REASON:"]
    for k, v in sorted(by_reason.items()):
        lines.append(f"  {k}={v}")
    lines.append("BY_MODULE:")
    for k, v in sorted(by_module.items()):
        lines.append(f"  {k}={v}")
    summary.write_text("\n".join(lines) + "\n")
    print(summary.read_text())
    if hits:
        print("--- stems ---")
        for path, reason in hits[:40]:
            print(f"  {reason}\t{path}")
        if len(hits) > 40:
            print(f"  ... +{len(hits)-40} more")
    return 0 if not hits else 1

if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    raise SystemExit(main(out))
