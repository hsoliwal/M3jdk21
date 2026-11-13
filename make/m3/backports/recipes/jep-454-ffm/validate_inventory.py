#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent
EXPECTED_PATHS = 261
EXPECTED_PLANES = {
    "ABI_LINKERS": 17,
    "BUILD_OR_MISC": 2,
    "FOREIGN_JTREG": 91,
    "INTERNAL_FOREIGN_RUNTIME": 11,
    "METHOD_HANDLE_VAR_HANDLE_INTEGRATION": 3,
    "MICROBENCH_EVIDENCE": 38,
    "MODULE_PREVIEW_NATIVE_ACCESS_POLICY": 5,
    "NATIVE_ACCESS_MANIFEST_TESTS": 8,
    "NATIVE_FALLBACK_LINKER": 1,
    "OTHER_TEST_CONSUMERS": 14,
    "PUBLIC_FOREIGN_API": 16,
    "VECTOR_AND_OTHER_CONSUMERS": 55,
}
EXPECTED_COMMITS = {
    "jdk21_preview_commit": "cbccc4c8172797ea2f1b7c301d00add3f517546d",
    "jdk22_final_commit": "32ac72c3d35138f5253e4defc948304ac3ea1b53",
    "jdk22_final_parent": "9728e21db1b35e487c562690de659aac386aa99d",
}


def read_pairs(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    return {row["field"]: row["value"] for row in rows}


def main() -> int:
    upstream = read_pairs(ROOT / "UPSTREAM.tsv")
    for key, expected in EXPECTED_COMMITS.items():
        actual = upstream.get(key)
        if actual != expected:
            raise SystemExit(f"{key}: expected {expected}, got {actual}")
    if upstream.get("finalization_path_count") != str(EXPECTED_PATHS):
        raise SystemExit("UPSTREAM finalization path count drift")

    with (ROOT / "PATH_PLANE.tsv").open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))

    if len(rows) != EXPECTED_PATHS:
        raise SystemExit(f"path denominator: expected {EXPECTED_PATHS}, got {len(rows)}")

    ordinals = [int(row["ordinal"]) for row in rows]
    if ordinals != list(range(EXPECTED_PATHS)):
        raise SystemExit("PATH_PLANE ordinals are not contiguous and stable")

    paths = [row["path"] for row in rows]
    if len(set(paths)) != len(paths):
        raise SystemExit("PATH_PLANE contains duplicate paths")

    for row in rows:
        if not row["upstream_status"]:
            raise SystemExit(f"missing upstream status: {row['path']}")
        if not row["plane"]:
            raise SystemExit(f"missing plane: {row['path']}")
        if row["upstream_status"] != "removed" and not row["upstream_blob"]:
            raise SystemExit(f"missing upstream blob: {row['path']}")
        for key in ("additions", "deletions", "changes"):
            if int(row[key]) < 0:
                raise SystemExit(f"negative {key}: {row['path']}")

    actual_planes = Counter(row["plane"] for row in rows)
    if dict(sorted(actual_planes.items())) != EXPECTED_PLANES:
        raise SystemExit(
            f"plane counts drift: expected {EXPECTED_PLANES}, got {dict(sorted(actual_planes.items()))}"
        )

    with (ROOT / "API_DELTA.tsv").open(encoding="utf-8", newline="") as stream:
        deltas = list(csv.DictReader(stream, delimiter="\t"))
    if len(deltas) != 15:
        raise SystemExit(f"API delta count: expected 15, got {len(deltas)}")
    if [int(row["ordinal"]) for row in deltas] != list(range(15)):
        raise SystemExit("API delta ordinals drift")
    if any(row["authority"] not in {"LIBRARY_API", "MODULE", "MULTI_MODULE"} for row in deltas):
        raise SystemExit("API delta authority outside admitted scope ladder")

    proof = read_pairs(ROOT / "PROOF_REQUEST.tsv")
    required = {
        "scope": "LIBRARY_API",
        "default_java21": "NO",
        "classification": "OPT_IN_SE_API_EXTENSION",
        "source_materialized": "false",
        "mutation_authority": "false",
        "promotion": "NOT_AUTHORIZED",
    }
    for key, expected in required.items():
        if proof.get(key) != expected:
            raise SystemExit(f"{key}: expected {expected}, got {proof.get(key)}")

    print("JEP454_INVENTORY=PASS")
    print(f"JEP454_PATHS={len(rows)}")
    for plane, count in sorted(actual_planes.items()):
        print(f"JEP454_PLANE_{plane}={count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
