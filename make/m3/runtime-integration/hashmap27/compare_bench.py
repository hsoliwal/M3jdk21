#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Match baseline and patched HashMap samples; report regressions as well as wins."""
import argparse
from pathlib import Path

def rows(path):
    found = {}
    for line in Path(path).read_text().splitlines():
        if "M3_HASHMAP_SAMPLE variant=" not in line or "_RAW" in line:
            continue
        data = dict(token.split("=", 1)
                    for token in line.split("M3_HASHMAP_SAMPLE ", 1)[1].split()
                    if "=" in token)
        key = (data["shape"], data["op"])
        if key in found:
            raise RuntimeError(f"duplicate sample {key}")
        found[key] = float(data["ns_per_op"])
    return found

p = argparse.ArgumentParser()
p.add_argument("before")
p.add_argument("after")
args = p.parse_args()
before = rows(args.before)
after = rows(args.after)
if not before or before.keys() != after.keys():
    raise SystemExit(f"M3_HASHMAP_BA_FAIL missing samples baseline={list(before)} "
                     f"patched={list(after)}")
for shape, operation in sorted(before):
    old, new = before[(shape, operation)], after[(shape, operation)]
    print(f"M3_HASHMAP_BEFORE_AFTER shape={shape} op={operation} "
          f"before_ns={old:.3f} after_ns={new:.3f} "
          f"speedup={old / new:.3f}x change_pct={(new / old - 1) * 100:.2f}% "
          f"matched_jvm=false")
