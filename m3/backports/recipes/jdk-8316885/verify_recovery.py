#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

UPSTREAM = "1230aed61d286fe9c09f46e2bab626d0e8fe0273"
MESSAGE = "No aggregated code heap data available. Run function aggregate first."


def rows(path: Path):
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def main() -> int:
    root = Path(__file__).resolve().parents[4]
    adaptation = rows(root / "m3/backports/recipes/jdk-8316885/adaptation.tsv")
    if len(adaptation) != 3:
        raise SystemExit(f"expected 3 adaptation rows, found {len(adaptation)}")

    for row in adaptation:
        if row["upstream_commit"] != UPSTREAM:
            raise SystemExit(f"donor drift: {row['target_path']}")
        path = root / row["target_path"]
        if not path.is_file():
            raise SystemExit(f"missing target: {row['target_path']}")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise SystemExit(f"target drift: {row['target_path']}: {actual}")

    seeds = rows(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((r for r in seeds if r["jbs"] == "JDK-8316885"), None)
    if seed is None or seed["upstream_commit"] != UPSTREAM:
        raise SystemExit("JDK-8316885 seed lineage missing")
    if seed["disposition"] != "candidate-adapted":
        raise SystemExit("JDK-8316885 must remain candidate-adapted")

    cpp = (root / "src/hotspot/share/code/codeHeapState.cpp").read_text(encoding="utf-8")
    hpp = (root / "src/hotspot/share/code/codeHeapState.hpp").read_text(encoding="utf-8")
    test = (
        root
        / "test/hotspot/jtreg/serviceability/dcmd/compiler/"
        / "CodeHeapAnalyticsMissingAggregate.java"
    ).read_text(encoding="utf-8")

    if MESSAGE not in cpp:
        raise SystemExit("missing generic aggregate diagnostic")
    if cpp.count("print_aggregate_missing(") < 13:
        raise SystemExit("not all detail paths are wired to aggregate diagnostic")
    if "static void print_aggregate_missing(outputStream* out, const char* heapName);" not in hpp:
        raise SystemExit("missing helper declaration")
    for function in (
        "UsedSpace",
        "FreeSpace",
        "MethodCount",
        "MethodSpace",
        "MethodAge",
        "MethodNames",
    ):
        if f'"{function}"' not in test:
            raise SystemExit(f"focused jtreg missing function {function}")
    if "Compiler.CodeHeap_Analytics aggregate" not in test:
        raise SystemExit("focused jtreg missing aggregate transition")
    if "shouldNotContain(MISSING)" not in test:
        raise SystemExit("focused jtreg missing post-aggregate negative assertion")

    print("PASS: JDK-8316885 current-tree diagnostic verifier")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
