#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
from pathlib import Path
import sys

SCHEMA = "M3JDK21_RECIPE_OWNERSHIP_V1"
ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite"
LEDGER = ROOT / "m3/docs/synexia-recipe-ownership-classification.tsv"

ALLOWED = {
    "SYNEXIA_CANONICAL_RESIDUE",
    "TARGET_ADAPTER_ONLY",
    "JDK_TARGET_SPECIFIC",
    "TARGET_ADAPTER_ONLY",
}


def java_files() -> set[str]:
    return {
        path.relative_to(ROOT).as_posix()
        for path in SOURCE_ROOT.rglob("*.java")
        if path.is_file()
    }


def read_rows() -> dict[str, dict[str, str]]:
    rows: dict[str, dict[str, str]] = {}
    with LEDGER.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        expected = {
            "schema",
            "local_path",
            "classification",
            "canonical_synexia_owner",
            "target_reason",
        }
        if set(reader.fieldnames or ()) != expected:
            raise SystemExit(f"unexpected ledger columns: {reader.fieldnames}")
        for row in reader:
            path = row["local_path"]
            if path in rows:
                raise SystemExit(f"duplicate ledger path: {path}")
            if row["schema"] != SCHEMA:
                raise SystemExit(f"invalid schema for {path}: {row['schema']}")
            classification = row["classification"]
            if classification not in ALLOWED:
                raise SystemExit(f"invalid classification for {path}: {classification}")
            owner = row["canonical_synexia_owner"].strip()
            reason = row["target_reason"].strip()
            if not reason:
                raise SystemExit(f"empty target_reason for {path}")
            if not owner or owner == "NONE":
                raise SystemExit(f"classification requires Synexia provenance owner: {path}")
            if not (owner.startswith("com.synexia.") or owner.startswith("com.synexia:")):
                raise SystemExit(f"non-Synexia provenance/canonical owner for {path}: {owner}")
            if classification == "JDK_TARGET_SPECIFIC" and not (
                owner.startswith("com.synexia.rewrite.")
                or owner.startswith("com.synexia:")
            ):
                raise SystemExit(
                    f"JDK_TARGET_SPECIFIC must name a Synexia planner/proof owner: {path}"
                )
            rows[path] = row
    return rows


def main() -> int:
    actual = java_files()
    rows = read_rows()
    classified = set(rows)

    missing = sorted(actual - classified)
    stale = sorted(classified - actual)
    if missing:
        raise SystemExit(
            "unclassified reusable/target recipe classes:\n" + "\n".join(missing)
        )
    if stale:
        raise SystemExit(
            "classification rows without current Java owner:\n" + "\n".join(stale)
        )

    counts = {key: 0 for key in sorted(ALLOWED)}
    for row in rows.values():
        counts[row["classification"]] += 1

    print(f"recipe ownership classification: PASS ({len(actual)} classes)")
    for key, value in counts.items():
        print(f"{key}={value}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
