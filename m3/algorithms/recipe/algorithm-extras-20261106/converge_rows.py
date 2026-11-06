#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Ledger rows of recipe/algorithm-extras-20261106 in m3/qualification/converge-collections-20261030/CONVERGE_COLLECTIONS.tsv.

One row per donor file of the donor catalogue directory (21): the FILE rows of RELOCATION_MAP.tsv as
PRESENT_IN_M3JDK at their relocated target, the four catalog files (SKIP + NEC rows) as PRESENT_IN_M3JDK at their
owner M3AlgorithmShapeCatalog. A row whose source is already in the ledger is updated in place; a new row is inserted
after the anchor (the progress-contract row of families-20261106), in donor path order. Idempotent: a second run
changes nothing; --check writes nothing and exits 1 when a run would change the ledger. Re-run after every rebase (the
ledger is shared with parallel leaves). The donor repository name, directory and anchor path are read from the REPO,
FILE and OWNED rows of RELOCATION_MAP.tsv, so no donor name is spelled here (jdk-naming fixed point).
usage: python -I converge_rows.py [--check]
"""
from __future__ import annotations

import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
LEDGER = RECIPE_DIR.parents[2] / "qualification" / "converge-collections-20261030" / "CONVERGE_COLLECTIONS.tsv"
MAP_ROWS = [line.split("\t") for line in (RECIPE_DIR / "RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines()]
REPO = next(row[1] for row in MAP_ROWS if row[0] == "REPO").rpartition("/")[2] + ":"
ANCHOR = REPO + next(row[1] for row in MAP_ROWS if row[0] == "OWNED") + "\t"
DONOR_DIR = next(row[1] for row in MAP_ROWS if row[0] == "FILE").rpartition("/")[0] + "/"
CATALOG = ("AlgorithmCatalog", "AlgorithmDescriptor", "AlgorithmShape", "AlgorithmTemplate")
CATALOG_OWNER = "m3/algorithms/src/main/java/jdk/m3/algorithm/M3AlgorithmShapeCatalog.java"


def wanted() -> list:
    rows = []
    for line in (RECIPE_DIR / "RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines():
        f = line.split("\t")
        if f[0] == "FILE" and f[1].startswith(DONOR_DIR):
            rows.append((REPO + f[1], Path(f[1]).stem, "m3/algorithms/" + f[2], "PRESENT_IN_M3JDK"))
    rows += [(REPO + DONOR_DIR + name + ".java", name, CATALOG_OWNER, "PRESENT_IN_M3JDK") for name in CATALOG]
    return sorted(rows)


def main(argv: list) -> int:
    check = "--check" in argv
    text = LEDGER.read_text(encoding="utf-8")
    lines = text.split("\n")
    changed = inserted = 0
    index = {line.split("\t", 1)[0]: i for i, line in enumerate(lines) if line}
    new_rows = []
    for row in wanted():
        line = "\t".join(row)
        if row[0] in index:
            if lines[index[row[0]]] != line:
                lines[index[row[0]]] = line
                changed += 1
        else:
            new_rows.append(line)
    if new_rows:
        anchors = [i for i, line in enumerate(lines) if line.startswith(ANCHOR)]
        if len(anchors) != 1:
            print("REFUSED: %d anchor rows %s in %s" % (len(anchors), ANCHOR.strip(), LEDGER))
            return 2
        lines[anchors[0] + 1:anchors[0] + 1] = new_rows
        inserted = len(new_rows)
    result = "\n".join(lines)
    states = {}
    for line in lines[1:]:
        if line:
            states[line.rsplit("\t", 1)[-1]] = states.get(line.rsplit("\t", 1)[-1], 0) + 1
    print("CONVERGE_ROWS inserted=%d updated=%d rows=%d states=%s %s" % (
        inserted, changed, len(wanted()), " ".join("%s=%d" % s for s in sorted(states.items())),
        "CHECK" if check else "WRITE"))
    if result != text and not check:
        LEDGER.write_bytes(result.encode("utf-8"))
    return 1 if check and result != text else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
