#!/usr/bin/env python3
"""Count tests, separating repetitive characterization batches from focused tests."""
from __future__ import annotations
import os, re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
MIN = int(os.environ.get("GUAVAKT_MIN_TESTS", "300"))
pat = re.compile(r"^\s*@Test\b", re.M)
total = batch = 0
for p in ROOT.glob("guavakt*/src/**/*.kt"):
    count = len(pat.findall(p.read_text(errors="replace")))
    total += count
    if "Characterization" in p.name and "Batch" in p.name:
        batch += count
substantive = total - batch
print(f"TOTAL_TESTS={total}")
print(f"CHARACTERIZATION_BATCH_TESTS={batch}")
print(f"SUBSTANTIVE_TESTS={substantive}")
print(f"MIN_SUBSTANTIVE_TESTS={MIN}")
if substantive < MIN:
    print(f"FAIL: need >= {MIN} non-batch @Test methods", file=sys.stderr)
    raise SystemExit(1)
raise SystemExit(0)
