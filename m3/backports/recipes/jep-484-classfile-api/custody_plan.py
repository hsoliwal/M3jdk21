#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Classify sealed JEP484 path-map rows against a current M3JDK21 checkout."""

from __future__ import annotations

import argparse
import csv
import hashlib
from collections import Counter
from pathlib import Path


PUBLIC = "src/java.base/share/classes/java/lang/classfile/"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def classify_mapped(root: Path, row: dict[str, str]) -> dict[str, str]:
    source_path = row["source_path"]
    donor_path = row["donor_path"]
    source = root / source_path
    donor = root / donor_path
    source_hash = row["source_sha256"]
    donor_hash = row["donor_sha256"]

    source_state = "MISSING"
    if source.is_file():
        source_state = "SEALED" if sha256(source) == source_hash else "DRIFT"

    if source_path == donor_path:
        if not source.is_file():
            state = "SOURCE_DRIFT"
        else:
            current = sha256(source)
            if current == donor_hash:
                state = "ALREADY_PRESENT"
            elif current == source_hash:
                state = "SAME_PATH_REPLACE"
            else:
                state = "SOURCE_DRIFT"
    else:
        if source_state != "SEALED":
            state = "SOURCE_DRIFT"
        elif not donor.exists():
            state = "MOVED_PUBLIC_ADD" if donor_path.startswith(PUBLIC) else "MOVED_INTERNAL_ADD"
        elif donor.is_file() and sha256(donor) == donor_hash:
            state = "ALREADY_PRESENT"
        else:
            state = "TARGET_OCCUPIED"

    return {
        "source_path": source_path,
        "source_sha256": source_hash,
        "donor_path": donor_path,
        "donor_sha256": donor_hash,
        "donor_plane": row["donor_plane"],
        "normalized_key": row["normalized_key"],
        "state": state,
        "source_state": source_state,
    }


def classify_added(root: Path, row: dict[str, str]) -> dict[str, str]:
    donor_path = row["donor_path"]
    donor = root / donor_path
    donor_hash = row["donor_sha256"]
    if not donor.exists():
        state = "ADD_DONOR_ONLY"
    elif donor.is_file() and sha256(donor) == donor_hash:
        state = "ALREADY_PRESENT"
    else:
        state = "TARGET_OCCUPIED"
    return {
        "source_path": "",
        "source_sha256": "",
        "donor_path": donor_path,
        "donor_sha256": donor_hash,
        "donor_plane": row["donor_plane"],
        "normalized_key": row["normalized_key"],
        "state": state,
        "source_state": "NOT_APPLICABLE",
    }


def classify_unmapped(root: Path, row: dict[str, str]) -> dict[str, str]:
    source_path = row["source_path"]
    source = root / source_path
    sealed = source.is_file() and sha256(source) == row["source_sha256"]
    return {
        "source_path": source_path,
        "source_sha256": row["source_sha256"],
        "donor_path": "",
        "donor_sha256": "",
        "donor_plane": "",
        "normalized_key": row["normalized_key"],
        "state": "RETAIN_UNMAPPED_SOURCE" if sealed else "SOURCE_DRIFT",
        "source_state": "SEALED" if sealed else "DRIFT_OR_MISSING",
    }


def materialize(root: Path, map_dir: Path, out: Path) -> list[dict[str, str]]:
    mapped = read_tsv(map_dir / "JDK21_INTERNAL_TO_JDK24.tsv")
    unmapped = read_tsv(map_dir / "UNMAPPED_JDK21.tsv")
    ambiguous = read_tsv(map_dir / "AMBIGUOUS.tsv")
    added = read_tsv(map_dir / "ADDED_JDK24.tsv")
    if ambiguous:
        raise ValueError(f"ambiguous JEP484 mappings are not admissible: {len(ambiguous)}")

    rows = [classify_mapped(root, row) for row in mapped]
    rows.extend(classify_unmapped(root, row) for row in unmapped)
    rows.extend(classify_added(root, row) for row in added)
    rows.sort(key=lambda row: (row["donor_path"] or row["source_path"], row["source_path"]))

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
    )
    with (out / "CUSTODY_PLAN.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)

    counts = Counter(row["state"] for row in rows)
    with (out / "CUSTODY_COUNTS.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("state", "count"))
        for state in sorted(counts):
            writer.writerow((state, counts[state]))

    signals = read_tsv(map_dir / "VERSION_SIGNALS.tsv")
    signal_paths = sorted({row["donor_path"] for row in signals})
    (out / "VERSION_SIGNAL_TARGETS.txt").write_text(
        "".join(path + "\n" for path in signal_paths), encoding="utf-8"
    )

    if len(rows) != len(mapped) + len(unmapped) + len(added):
        raise AssertionError("JEP484 custody accounting drift")
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target-root", type=Path, required=True)
    parser.add_argument("--map-dir", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    rows = materialize(args.target_root.resolve(), args.map_dir.resolve(), args.out.resolve())
    print(f"JEP484_CUSTODY_ROWS={len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
