#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Differential gate: oracle (switch_consumers.py copy with --out-dir) vs recipe (M3VendoredPinApply).

Compares every output pom byte for byte (over the union of files under both pom dirs) and the rows TSVs row for
row (same order). Prints a one-line summary and one line per difference; exit 0 only when identical.
usage: compare.py <side> <oracle_poms> <oracle_rows> <recipe_poms> <recipe_rows>
"""
from __future__ import annotations
import difflib, hashlib, sys
from pathlib import Path


def files(root: Path) -> dict[str, bytes]:
    return {p.relative_to(root).as_posix(): p.read_bytes() for p in sorted(root.rglob("*")) if p.is_file()}


def main(side: str, op: str, orows: str, rp: str, rrows: str) -> int:
    a, b = files(Path(op)), files(Path(rp))
    diffs = []
    for k in sorted(set(a) | set(b)):
        if k not in a or k not in b:
            diffs.append("POM_MISSING\t%s\toracle=%s recipe=%s" % (k, k in a, k in b)); continue
        if a[k] != b[k]:
            i = next(i for i in range(min(len(a[k]), len(b[k])) + 1) if i >= min(len(a[k]), len(b[k])) or a[k][i] != b[k][i])
            diffs.append("POM_BYTES\t%s\tfirst_diff_offset=%d oracle=%r recipe=%r" % (k, i, a[k][max(0, i - 20):i + 40], b[k][max(0, i - 20):i + 40]))
    ra = Path(orows).read_bytes().decode("utf-8").split("\n")
    rb = Path(rrows).read_bytes().decode("utf-8").split("\n")
    for line in difflib.unified_diff(ra, rb, "oracle_rows", "recipe_rows", n=0, lineterm=""):
        if not line.startswith(("---", "+++")):
            diffs.append("ROW\t" + line)
    same = sum(1 for k in set(a) & set(b) if a[k] == b[k])
    print("%s\tpoms_oracle=%d\tpoms_recipe=%d\tidentical=%d\tdifferent=%d\trows_oracle=%d\trows_recipe=%d\trows_bytes_equal=%s\toracle_rows_sha256=%s\trecipe_rows_sha256=%s"
          % (side, len(a), len(b), same, len(set(a) | set(b)) - same, len(ra) - 2, len(rb) - 2, Path(orows).read_bytes() == Path(rrows).read_bytes(),
             hashlib.sha256(Path(orows).read_bytes()).hexdigest()[:16], hashlib.sha256(Path(rrows).read_bytes()).hexdigest()[:16]))
    for d in diffs:
        print(d)
    return 0 if not diffs else 1


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:6]))
