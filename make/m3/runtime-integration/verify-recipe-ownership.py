#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
from pathlib import Path, PurePosixPath
import sys

SCHEMA = "M3JDK21_RECIPE_OWNERSHIP_V1"
ROOT = Path(__file__).resolve().parents[3]
SOURCE_PATH = PurePosixPath("make/m3/tooling/migration-recipes/src/main/java")
LEDGER = ROOT / "doc/m3/m3-recipe-ownership-classification.tsv"

# These are source-policy identities in the existing receiving ledger, not
# target Java names. Renaming them severs the canonical ownership record.
COLUMNS = (
    "schema",
    "local_path",
    "classification",
    "canonical_synexia_owner",
    "target_reason",
)

ALLOWED = {
    "SYNEXIA_CANONICAL_RESIDUE",
    "TARGET_ADAPTER_ONLY",
    "JDK_TARGET_SPECIFIC",
}


def java_files(root: Path = ROOT) -> set[str]:
    source_root = root / SOURCE_PATH
    if not source_root.is_dir() or source_root.is_symlink():
        raise SystemExit(f"missing or indirect recipe source root: {SOURCE_PATH}")
    actual: set[str] = set()
    for path in source_root.rglob("*"):
        relative = path.relative_to(root).as_posix()
        if path.is_symlink():
            raise SystemExit(f"indirect recipe source path: {relative}")
        if path.is_file() and path.suffix == ".java":
            actual.add(relative)
    return actual


def read_rows(ledger: Path = LEDGER) -> dict[str, dict[str, str]]:
    rows: dict[str, dict[str, str]] = {}
    with ledger.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if tuple(reader.fieldnames or ()) != COLUMNS:
            raise SystemExit(f"unexpected ledger columns: {reader.fieldnames}")
        for line, row in enumerate(reader, start=2):
            if None in row or any(value is None for value in row.values()):
                raise SystemExit(f"malformed ledger row at line {line}")
            path = row["local_path"]
            local = PurePosixPath(path)
            if (
                path != local.as_posix()
                or "\\" in path
                or ".." in local.parts
                or not local.is_relative_to(SOURCE_PATH)
                or local.suffix != ".java"
            ):
                raise SystemExit(f"invalid recipe source path: {path}")
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


def verify(root: Path = ROOT, ledger: Path = LEDGER) -> dict[str, int]:
    actual = java_files(root)
    rows = read_rows(ledger)
    classified = set(rows)

    missing = sorted(actual - classified)
    stale = sorted(classified - actual)
    failures: list[str] = []
    if missing:
        failures.append("unclassified reusable/target recipe classes:\n" + "\n".join(missing))
    if stale:
        failures.append("classification rows without current Java owner:\n" + "\n".join(stale))
    if failures:
        raise SystemExit("\n\n".join(failures))

    counts = {key: 0 for key in sorted(ALLOWED)}
    for row in rows.values():
        counts[row["classification"]] += 1

    return counts


def main() -> int:
    counts = verify()
    print(f"recipe ownership classification: PASS ({sum(counts.values())} classes)")
    for key, value in counts.items():
        print(f"{key}={value}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
