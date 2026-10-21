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
      "hash-pinned-java/m3-file-layered-facts-20261020"
)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tsv(path: Path):
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.reader(stream, delimiter="\t"))


def main() -> None:
    manifest = tsv(CRATE / "manifest.tsv")
    checked = 0
    seen: set[str] = set()
    for row in manifest:
        if not row or row[0].startswith("#"):
            continue
        if len(row) != 4:
            raise SystemExit("invalid layered-facts manifest row")
        relative, before, after, resource = row
        if relative in seen:
            raise SystemExit(f"duplicate target: {relative}")
        seen.add(relative)
        if before != "ABSENT" and len(before) != 64:
            raise SystemExit(f"invalid preimage digest: {relative}")
        if len(after) != 64:
            raise SystemExit(f"invalid postimage digest: {relative}")
        target = ROOT / relative
        template = CRATE / resource
        if digest(target) != after or digest(template) != after:
            raise SystemExit(f"postimage drift: {relative}")
        if target.read_bytes() != template.read_bytes():
            raise SystemExit(f"target/template mismatch: {relative}")
        checked += 1
    if checked != 8:
        raise SystemExit(f"expected 8 layered-fact targets, got {checked}")

    memory = tsv(CRATE / "RECIPE_MEMORY.tsv")
    header = [
        "utc", "recipe", "trigger", "observed", "correction",
        "corrected_by", "guard", "lesson",
    ]
    if not memory or memory[0] != header or len(memory) < 3:
        raise SystemExit("invalid layered-facts recipe memory")
    for row in memory[1:]:
        if len(row) != 8 or any(not cell for cell in row):
            raise SystemExit("invalid layered-facts memory row")
    if not any(
        row[3] == "M3_FILE_FACT_IDENTICAL_DUPLICATE_SHOULD_CONVERGE"
        and "identicalFactReceiptsConvergeButConflictsFail" in row[6]
        for row in memory[1:]
    ):
        raise SystemExit("missing duplicate-convergence correction memory")

    catalogue = tsv(ROOT / "m3/file/qualification/layered-facts-20261020/ALGORITHM_CATALOGUE.tsv")
    if catalogue[0] != [
        "category", "problem_shape", "selected", "complexity", "category_reference"
    ] or len(catalogue) < 5:
        raise SystemExit("invalid layered-facts algorithm catalogue")

    print(
        f"PASS M3_FILE_LAYERED_FACTS_RECIPE targets={checked} "
        f"memory_rows={len(memory) - 1}"
    )


if __name__ == "__main__":
    main()
