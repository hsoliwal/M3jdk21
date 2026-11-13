#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""jsondiff.py <before root> <after root> <path...> : leaf-level JSON differences (key path: before -> after)."""
import json, sys
from pathlib import Path
def leaves(v, p=""):
    if isinstance(v, dict):
        for k in v: yield from leaves(v[k], f"{p}.{k}")
    elif isinstance(v, list):
        key = lambda x: x.get("repository", json.dumps(x, sort_keys=True)) if isinstance(x, dict) else json.dumps(x)
        for x in v: yield from leaves(x, f"{p}[{key(x)}]")
    else:
        yield p, v
a, b = Path(sys.argv[1]), Path(sys.argv[2])
for rel in sys.argv[3:]:
    x, y = dict(leaves(json.loads((a / rel).read_text("utf-8")))), dict(leaves(json.loads((b / rel).read_text("utf-8"))))
    keys = sorted(set(x) | set(y))
    kinds = {}
    for k in keys:
        if x.get(k) != y.get(k):
            tail = k.rsplit(".", 1)[-1]
            kinds[tail] = kinds.get(tail, 0) + 1
    print(rel, kinds)
