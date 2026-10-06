#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed ownership check for Synexia-canonical reusable recipe mirrors."""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = Path(__file__).with_name("synexia-recipe-ownership.tsv")
HEX40 = re.compile(r"[0-9a-f]{40}")


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def load(path: Path = LEDGER) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        expected = [
            "family",
            "legacy_path",
            "legacy_git_blob",
            "canonical_path",
            "synexia_revision",
            "synexia_git_blob",
            "state",
        ]
        if reader.fieldnames != expected:
            raise ValueError("invalid Synexia recipe ownership header")
        rows = list(reader)

    if not rows:
        raise ValueError("empty Synexia recipe ownership ledger")

    families: set[str] = set()
    revisions: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in expected):
            raise ValueError(f"blank ownership field at row {physical}")
        if row["family"] in families:
            raise ValueError(f"duplicate recipe family {row['family']}")
        families.add(row["family"])
        if row["state"] != "BORROWED_CANONICAL_WITH_FROZEN_LEGACY":
            raise ValueError(f"invalid recipe ownership state at row {physical}")
        if not HEX40.fullmatch(row["synexia_revision"]):
            raise ValueError(f"invalid Synexia revision at row {physical}")
        revisions.add(row["synexia_revision"])
        for field in ("legacy_git_blob", "synexia_git_blob"):
            if not HEX40.fullmatch(row[field]):
                raise ValueError(f"invalid Git blob {field} at row {physical}")
        if not row["legacy_path"].startswith(
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/"
        ):
            raise ValueError(f"legacy recipe escaped frozen namespace at row {physical}")
        if not row["canonical_path"].startswith(
            "m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/atom/"
        ):
            raise ValueError(f"canonical recipe escaped borrowed namespace at row {physical}")

        legacy = ROOT / row["legacy_path"]
        canonical = ROOT / row["canonical_path"]
        if not legacy.is_file() or git_blob(legacy) != row["legacy_git_blob"]:
            raise ValueError(f"frozen legacy recipe drift: {row['legacy_path']}")
        if not canonical.is_file() or git_blob(canonical) != row["synexia_git_blob"]:
            raise ValueError(f"Synexia canonical mirror drift: {row['canonical_path']}")

    if len(revisions) != 1:
        raise ValueError("one handoff must bind one exact Synexia revision")
    return rows


def main(argv: list[str]) -> int:
    if len(argv) > 1:
        print("usage: check_synexia_recipe_ownership.py", file=sys.stderr)
        return 2
    rows = load()
    revision = rows[0]["synexia_revision"]
    print(f"SYNEXIA_RECIPE_OWNERSHIP_PASS rows={len(rows)} revision={revision}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
