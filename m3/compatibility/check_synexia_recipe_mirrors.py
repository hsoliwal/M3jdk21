#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed verifier for non-canonical M3JDK recipe mirrors."""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
MANIFEST = HERE / "synexia-recipe-mirrors.tsv"
SCHEMA = "M3_SYNEXIA_RECIPE_MIRROR_V1"
SOURCE_REPOSITORY = "hsoliwal/com.synexia"
FAMILY = "PURE_INT_FILE_CONVERGENCE"
TRANSFORM = "PACKAGE_PREFIX_ONLY"
STATUS = "NONCANONICAL_MIRROR"
HEX40 = re.compile(r"[0-9a-f]{40}")

HEADER = [
    "schema",
    "family",
    "source_repository",
    "source_revision",
    "canonical_path",
    "canonical_blob",
    "target_path",
    "target_blob",
    "mirror_transform",
    "status",
]


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def validate(root: Path, manifest: Path = MANIFEST) -> int:
    with manifest.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != HEADER:
            raise ValueError("invalid Synexia recipe mirror header")
        rows = list(reader)

    if len(rows) != 7:
        raise ValueError(f"expected exactly seven canonical pure-int mirrors, got {len(rows)}")

    seen_targets: set[str] = set()
    seen_canonical: set[str] = set()
    revisions: set[str] = set()

    for index, row in enumerate(rows, start=2):
        if row["schema"] != SCHEMA or row["family"] != FAMILY:
            raise ValueError(f"invalid mirror identity at row {index}")
        if row["source_repository"] != SOURCE_REPOSITORY:
            raise ValueError(f"invalid canonical repository at row {index}")
        if not HEX40.fullmatch(row["source_revision"]):
            raise ValueError(f"invalid canonical revision at row {index}")
        if not HEX40.fullmatch(row["canonical_blob"]) or not HEX40.fullmatch(row["target_blob"]):
            raise ValueError(f"invalid blob seal at row {index}")
        if row["mirror_transform"] != TRANSFORM or row["status"] != STATUS:
            raise ValueError(f"invalid mirror authority at row {index}")

        canonical = row["canonical_path"]
        target = row["target_path"]
        if canonical in seen_canonical or target in seen_targets:
            raise ValueError(f"duplicate mirror row at {index}")
        seen_canonical.add(canonical)
        seen_targets.add(target)
        revisions.add(row["source_revision"])

        if not canonical.startswith(
            "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/atom/"
        ):
            raise ValueError(f"noncanonical Synexia recipe path at row {index}")
        if not target.startswith(
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/"
        ):
            raise ValueError(f"noncanonical M3JDK mirror path at row {index}")

        file = (root / target).resolve()
        if not file.is_file():
            raise ValueError(f"missing recipe mirror: {target}")
        actual = git_blob(file)
        if actual != row["target_blob"]:
            raise ValueError(
                f"recipe mirror drift: {target}: expected {row['target_blob']} actual {actual}"
            )

        text = file.read_text(encoding="utf-8")
        if "package com.m3.rewrite.atom;" not in text:
            raise ValueError(f"target mirror package drift: {target}")

    if len(revisions) != 1:
        raise ValueError("pure-int mirror family must bind one Synexia revision")

    return len(rows)


def main(argv: list[str]) -> int:
    if len(argv) > 2:
        print("usage: check_synexia_recipe_mirrors.py [repository-root]", file=sys.stderr)
        return 2
    root = Path(argv[1]).resolve() if len(argv) == 2 else HERE.parents[1]
    count = validate(root)
    print(f"SYNEXIA_RECIPE_MIRROR_PASS rows={count} canonical={SOURCE_REPOSITORY}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
