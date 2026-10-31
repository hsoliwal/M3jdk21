#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Scope-separate JEP484 custody rows before any source-changing recipe is authored."""

from __future__ import annotations

import argparse
import csv
from collections import Counter
from pathlib import Path

PUBLIC = "JDK24_PUBLIC"
INTERNAL = "JDK24_INTERNAL"

FILE_CANDIDATE = "FILE_CANDIDATE"
MODULE_CANDIDATE = "MODULE_CANDIDATE"
LIBRARY_API_HOLD = "LIBRARY_API_HOLD"
VERSION_ADAPTATION_HOLD = "VERSION_ADAPTATION_HOLD"
CURRENT_TREE_HOLD = "CURRENT_TREE_HOLD"
RETAIN_JDK21_SOURCE = "RETAIN_JDK21_SOURCE"
ALREADY_PRESENT_PROOF = "ALREADY_PRESENT_PROOF"

MUTATING_CANDIDATES = {FILE_CANDIDATE, MODULE_CANDIDATE}


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def version_signal_paths(map_dir: Path) -> set[str]:
    return {
        row["donor_path"]
        for row in read_tsv(map_dir / "VERSION_SIGNALS.tsv")
        if row["donor_path"]
    }


def classify(row: dict[str, str], signal_paths: set[str]) -> tuple[str, str, str]:
    state = row["state"]
    donor_path = row["donor_path"]
    donor_plane = row["donor_plane"]

    if state == "RETAIN_UNMAPPED_SOURCE":
        return RETAIN_JDK21_SOURCE, "NONE", "JDK21 internal has no direct JDK24 descendant"

    if state in {"SOURCE_DRIFT", "TARGET_OCCUPIED"}:
        return CURRENT_TREE_HOLD, "NONE", "current target must be reconciled before recipe authoring"

    if state == "ALREADY_PRESENT":
        return ALREADY_PRESENT_PROOF, "NONE", "reviewed donor bytes are already present"

    if donor_plane == PUBLIC:
        return (
            LIBRARY_API_HOLD,
            "LIBRARY_API",
            "java.lang.classfile is a post-21 public SE API and cannot masquerade as stock Java21",
        )

    if donor_path in signal_paths:
        return (
            VERSION_ADAPTATION_HOLD,
            "MODULE",
            "donor references Java 22-24 source/classfile-version semantics",
        )

    if donor_plane != INTERNAL:
        return CURRENT_TREE_HOLD, "NONE", "unknown donor plane"

    if state == "SAME_PATH_REPLACE":
        return (
            FILE_CANDIDATE,
            "FILE",
            "sealed internal same-path descendant with no post-21 version signal",
        )

    if state in {"MOVED_INTERNAL_ADD", "ADD_DONOR_ONLY"}:
        return (
            MODULE_CANDIDATE,
            "MODULE",
            "internal addition/relocation needs module composition after FILE proof",
        )

    return CURRENT_TREE_HOLD, "NONE", f"unclassified custody state {state}"


def plan(map_dir: Path, custody_dir: Path, out: Path) -> list[dict[str, str]]:
    rows = read_tsv(custody_dir / "CUSTODY_PLAN.tsv")
    signals = version_signal_paths(map_dir)
    planned: list[dict[str, str]] = []

    for row in rows:
        disposition, scope, reason = classify(row, signals)
        planned.append(
            {
                **row,
                "disposition": disposition,
                "scope": scope,
                "reason": reason,
                "mutation_authority": "false",
                "promotion_authority": "false",
            }
        )

    planned.sort(key=lambda row: (row["donor_path"] or row["source_path"], row["source_path"]))
    out.mkdir(parents=True, exist_ok=True)
    fields = (
        "source_path",
        "source_sha256",
        "donor_path",
        "donor_sha256",
        "donor_plane",
        "normalized_key",
        "state",
        "source_state",
        "disposition",
        "scope",
        "reason",
        "mutation_authority",
        "promotion_authority",
    )
    with (out / "SCOPE_PLAN.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(planned)

    counts = Counter(row["disposition"] for row in planned)
    with (out / "SCOPE_COUNTS.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("disposition", "count"))
        for key in sorted(counts):
            writer.writerow((key, counts[key]))

    file_candidates = [row for row in planned if row["disposition"] == FILE_CANDIDATE]
    module_candidates = [row for row in planned if row["disposition"] == MODULE_CANDIDATE]
    for name, candidates in (
        ("FILE_CANDIDATES.txt", file_candidates),
        ("MODULE_CANDIDATES.txt", module_candidates),
    ):
        (out / name).write_text(
            "".join((row["donor_path"] or row["source_path"]) + "\n" for row in candidates),
            encoding="utf-8",
        )

    if len(planned) != len(rows):
        raise AssertionError("JEP484 scope accounting drift")
    if any(row["mutation_authority"] != "false" for row in planned):
        raise AssertionError("planning cannot grant mutation authority")
    return planned


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--map-dir", type=Path, required=True)
    parser.add_argument("--custody-dir", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    rows = plan(args.map_dir.resolve(), args.custody_dir.resolve(), args.out.resolve())
    counts = Counter(row["disposition"] for row in rows)
    print("JEP484_SCOPE", dict(sorted(counts.items())))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
