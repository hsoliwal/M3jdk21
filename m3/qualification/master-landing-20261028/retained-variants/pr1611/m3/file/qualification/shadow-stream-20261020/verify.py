#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
CRATE = (
    ROOT
    / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/"
      "hash-pinned-java/m3-file-shadow-stream-delta-20261020"
)
MANIFEST = CRATE / "manifest.tsv"
MEMORY = CRATE / "RECIPE_MEMORY.tsv"

EXPECTED_MEMORY = [
    "utc",
    "recipe",
    "trigger",
    "observed",
    "correction",
    "corrected_by",
    "guard",
    "lesson",
]


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rows(path: Path):
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.reader(stream, delimiter="\t"))


def verify_manifest() -> int:
    checked = 0
    seen: set[str] = set()
    for row in rows(MANIFEST):
        if not row or row[0].startswith("#"):
            continue
        if len(row) != 4:
            raise SystemExit("invalid manifest column count")
        relative, before, after, resource = row
        if relative in seen:
            raise SystemExit(f"duplicate manifest target: {relative}")
        seen.add(relative)
        if before != "ABSENT" and len(before) != 64:
            raise SystemExit(f"invalid before digest: {relative}")
        if len(after) != 64:
            raise SystemExit(f"invalid after digest: {relative}")
        target = ROOT / relative
        template = CRATE / resource
        if not target.is_file() or not template.is_file():
            raise SystemExit(f"missing target/template: {relative}")
        actual_target = digest(target)
        actual_template = digest(template)
        if actual_target != after:
            raise SystemExit(
                f"target postimage drift: {relative}: {actual_target} != {after}"
            )
        if actual_template != after:
            raise SystemExit(
                f"recipe postimage drift: {relative}: {actual_template} != {after}"
            )
        if target.read_bytes() != template.read_bytes():
            raise SystemExit(f"target/template bytes differ: {relative}")
        checked += 1
    if checked != 9:
        raise SystemExit(f"expected 9 recipe targets, got {checked}")
    return checked


def verify_memory() -> int:
    data = rows(MEMORY)
    if not data or data[0] != EXPECTED_MEMORY:
        raise SystemExit("invalid RECIPE_MEMORY.tsv header")
    if len(data) < 3:
        raise SystemExit("recipe memory must contain initial and correction rows")
    seen: set[tuple[str, str, str]] = set()
    for index, row in enumerate(data[1:], start=2):
        if len(row) != len(EXPECTED_MEMORY) or any(not cell for cell in row):
            raise SystemExit(f"invalid recipe memory row {index}")
        key = (row[0], row[2], row[3])
        if key in seen:
            raise SystemExit(f"duplicate recipe memory row {index}")
        seen.add(key)
        if "M3FileStream" not in row[6] and "M3FileStreamDelta" not in row[6]:
            raise SystemExit(f"memory row lacks executable guard {index}")
    return len(data) - 1


def verify_catalogues() -> None:
    donor = ROOT / "m3/file/qualification/shadow-stream-20261020/DONORS.tsv"
    catalogue = ROOT / "m3/file/qualification/shadow-stream-20261020/ALGORITHM_CATALOGUE.tsv"
    donor_rows = rows(donor)
    algorithm_rows = rows(catalogue)
    if len(donor_rows) < 5 or donor_rows[0] != [
        "category", "donor", "revision", "license", "decision", "mechanism"
    ]:
        raise SystemExit("invalid donor catalogue")
    if len(algorithm_rows) < 5 or algorithm_rows[0] != [
        "category", "problem_shape", "selected", "complexity", "rationale"
    ]:
        raise SystemExit("invalid algorithm catalogue")
    if any(row[4] not in {"REFERENCE_ONLY", "REUSE_EXISTING", "CATEGORY_ONLY"}
           for row in donor_rows[1:]):
        raise SystemExit("unreviewed donor decision")


def main() -> None:
    product = verify_manifest()
    memory = verify_memory()
    verify_catalogues()
    print(f"PASS M3_FILE_STREAM_DELTA_RECIPE targets={product} memory_rows={memory}")


if __name__ == "__main__":
    main()
