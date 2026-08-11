#!/usr/bin/env python3
"""Rough port depth status for guavakt commonMain files."""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FILTER = sys.argv[1].lower() if len(sys.argv) > 1 else ""

THIN = re.compile(
    r"map backed by LinkedHashMap|delegates to ArrayListMultimap|ListenableFuture via SettableFuture|"
    r"Hashing\.murmur3_32\(\)|static utilities \(KMP port\)|object identity \+",
    re.I,
)
SHORT = 25  # lines → likely thin

def main() -> None:
    rows = []
    for p in sorted(ROOT.glob("guavakt-*/src/commonMain/**/*.kt")):
        mod = p.parts[0]
        if FILTER and FILTER not in mod.lower() and FILTER not in str(p).lower():
            continue
        text = p.read_text(errors="replace")
        lines = text.count("\n") + 1
        thin = bool(THIN.search(text)) or lines <= SHORT
        # exclude annotations / tiny markers
        if "annotation class" in text and lines < 40:
            thin = False
        rows.append((thin, lines, str(p.relative_to(ROOT))))

    thin_rows = [r for r in rows if r[0]]
    fat_rows = [r for r in rows if not r[0]]
    print(f"files={len(rows)}  likely_thin={len(thin_rows)}  okish={len(fat_rows)}")
    print("\n-- likely thin (port next) --")
    for _, lines, path in sorted(thin_rows, key=lambda x: (-x[1], x[2]))[:40]:
        print(f"  {lines:4d}  {path}")
    if len(thin_rows) > 40:
        print(f"  ... +{len(thin_rows) - 40} more")

if __name__ == "__main__":
    main()
    print('DEPTH_SEE=scripts/depth_inventory.py')
