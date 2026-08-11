#!/usr/bin/env python3
"""Fail if any type whose name contains Immutable still exposes live mutation."""
from __future__ import annotations
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
bad = []
for p in ROOT.glob("guavakt-*/src/commonMain/**/*Immutable*.kt"):
    text = p.read_text(errors="replace")
    abstract_mutable = re.search(r":\s*AbstractMutable", text)
    live_mutator = re.search(
        r"override fun (put|add|removeAt|set|remove|clear)\b[^\n]*(delegate\.|\{\s*delegate\.)",
        text,
    )
    if abstract_mutable and (live_mutator or "UnsupportedOperationException" not in text):
        bad.append(str(p.relative_to(ROOT)))
    if re.search(r"override fun put\([^)]*\)[^{]*\{\s*delegate\.put", text, re.S):
        bad.append(f"{p.relative_to(ROOT)}:live_put")
    if re.search(r"override fun put\([^)]*\):\s*\w+\??\s*=\s*delegate\.put", text):
        bad.append(f"{p.relative_to(ROOT)}:live_put_expr")

if bad:
    print("IMMUTABLE_AUDIT_FAIL")
    for b in bad:
        print(b)
    sys.exit(1)
print("IMMUTABLE_AUDIT_OK")
sys.exit(0)
