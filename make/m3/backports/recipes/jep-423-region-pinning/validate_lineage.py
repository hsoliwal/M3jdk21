#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate the complete required JEP 423 Region Pinning lineage packet."""

from __future__ import annotations

import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
LINEAGE = ROOT / "LINEAGE.tsv"
PATHS = ROOT / "PATH_CLOSURE.tsv"
SUMMARY = ROOT / "LINEAGE_SUMMARY.json"
CANDIDATES = ROOT / "CUMULATIVE_ADMIT_PATHS.txt"

REQUIRED = (
    ("8318706", "38cfb220ddadbb401cc15f313aadb8234f626210", "JEP_IMPLEMENTATION"),
    ("8323610", "8643cc21333c6b51242ed3b9295b25f372244755", "PIN_COUNT_OVERFLOW_REPAIR"),
    ("8322484", "0d5f5e15d43f94a79c6133baecd5af217365d176", "PIN_CACHE_REGRESSION_REPAIR"),
)

PRESERVED = {
    "test/hotspot/jtreg/gc/stress/TestJNIBlockFullGC/TestJNIBlockFullGC.java",
    "test/hotspot/jtreg/gc/stress/TestJNIBlockFullGC/libTestJNIBlockFullGC.c",
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def path_list(path: Path) -> list[str]:
    values = [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if values != sorted(values) or len(values) != len(set(values)):
        raise ValueError(f"path list must be unique and sorted: {path.name}")
    return values


def validate() -> dict[str, int]:
    lineage = rows(LINEAGE)
    if len(lineage) != len(REQUIRED):
        raise ValueError(f"expected {len(REQUIRED)} lineage rows, got {len(lineage)}")
    for order, (row, expected) in enumerate(zip(lineage, REQUIRED, strict=True)):
        jbs, commit, role = expected
        if int(row["order"]) != order:
            raise ValueError(f"lineage order drift at {jbs}")
        if (row["jbs"], row["commit"], row["role"]) != expected:
            raise ValueError(f"lineage identity drift at order {order}")
        if row["required"] != "true" or row["promotion_authority"] != "false":
            raise ValueError(f"authority drift at {jbs}")

    closure = rows(PATHS)
    if not closure:
        raise ValueError("empty path closure")
    paths = [row["path"] for row in closure]
    if paths != sorted(paths) or len(paths) != len(set(paths)):
        raise ValueError("path closure must be unique and lexicographically sorted")

    by_path = {row["path"]: row for row in closure}
    if not PRESERVED.issubset(by_path):
        raise ValueError("legacy Java21 JNI/full-GC tests missing from closure")
    for path in PRESERVED:
        row = by_path[path]
        if row["java21_policy"] != "PRESERVE_JAVA21_PATH":
            raise ValueError(f"legacy Java21 deletion was silently admitted: {path}")
        if "removed" not in row["statuses"].split(","):
            raise ValueError(f"preserved path is not an upstream removal: {path}")

    candidates = path_list(CANDIDATES)
    expected_candidates = sorted(set(paths) - PRESERVED)
    if candidates != expected_candidates:
        raise ValueError(
            "cumulative candidate path set does not equal closure minus preserved deletions"
        )
    if len(candidates) != 62:
        raise ValueError(f"expected 62 cumulative candidate paths, got {len(candidates)}")

    required_commits = {commit for _, commit, _ in REQUIRED}
    observed_commits: set[str] = set()
    for row in closure:
        observed_commits.update(row["commits"].split(","))
        first = int(row["first_order"])
        last = int(row["last_order"])
        if first < 0 or last < first or last >= len(REQUIRED):
            raise ValueError(f"invalid order range for {row['path']}")
    if observed_commits != required_commits:
        raise ValueError("path closure does not bind the exact required commit set")

    summary = json.loads(SUMMARY.read_text(encoding="utf-8"))
    expected_summary = {
        "lineage_commits": len(REQUIRED),
        "unique_paths": len(closure),
        "preserved_java21_deletions": len(PRESERVED),
        "product_paths": sum(path.startswith("src/") for path in paths),
        "test_paths": sum(path.startswith("test/") for path in paths),
    }
    if summary != expected_summary:
        raise ValueError(f"summary drift: {summary!r} != {expected_summary!r}")
    return expected_summary


if __name__ == "__main__":
    print(json.dumps(validate(), sort_keys=True))
