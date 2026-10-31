#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bind the checked-out J485 tree to its source-sealed FILE-atom catalogue."""

from __future__ import annotations

import csv
import hashlib
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Row:
    crate: str
    path: str
    mode: str
    before_sha256: str
    after_sha256: str
    before_resource: str
    after_resource: str


def sha256(path: Path) -> str:
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def read_rows(catalogue: Path) -> list[Row]:
    with catalogue.open("r", encoding="utf-8", newline="") as handle:
        values = list(csv.DictReader(handle, delimiter="\t"))
    rows = [Row(**value) for value in values]
    if not rows:
        raise ValueError("empty J485 current catalogue")
    if len({row.crate for row in rows}) != len(rows):
        raise ValueError("duplicate J485 crate")
    if len({row.path for row in rows}) != len(rows):
        raise ValueError("duplicate J485 path")
    for row in rows:
        if row.mode not in {"GUARD", "MUTATE"}:
            raise ValueError("invalid J485 mode: " + row.mode)
        if not row.crate.startswith("jdk24-j485-cur-"):
            raise ValueError("invalid J485 crate: " + row.crate)
        if not (row.path.startswith("src/java.base/share/classes/java/util/stream/")
                or row.path.startswith("test/jdk/java/util/stream/")):
            raise ValueError("out-of-scope J485 path: " + row.path)
    return rows


def verify(
    repo: Path,
    *,
    expected_rows: int = 15,
    expected_guards: int = 8,
    expected_mutations: int = 7,
) -> tuple[int, int]:
    root = repo.resolve(strict=True)
    resource_root = root / (
        "m3/tooling/migration-recipes/src/main/resources/"
        "com/m3/rewrite/backport/jdk21-hash-pinned"
    )
    catalogue = resource_root / "j485-current-catalogue.tsv"
    rows = read_rows(catalogue)
    if len(rows) != expected_rows:
        raise ValueError(f"expected {expected_rows} J485 rows, found {len(rows)}")

    guards = 0
    mutations = 0
    for row in rows:
        actual = (root / row.path).resolve(strict=True)
        if not actual.is_relative_to(root) or actual.is_symlink() or not actual.is_file():
            raise ValueError("unsafe J485 target: " + row.path)

        crate = resource_root / row.crate
        before = (crate / row.before_resource).resolve(strict=True)
        after = (crate / row.after_resource).resolve(strict=True)
        if not before.is_relative_to(resource_root) or not after.is_relative_to(resource_root):
            raise ValueError("J485 resource escaped root: " + row.crate)
        if sha256(before) != row.before_sha256:
            raise ValueError("J485 before-resource drift: " + row.path)
        if sha256(after) != row.after_sha256:
            raise ValueError("J485 after-resource drift: " + row.path)
        if sha256(actual) != row.after_sha256:
            raise ValueError("J485 current-tree drift: " + row.path)

        if row.mode == "GUARD":
            guards += 1
            if row.before_sha256 != row.after_sha256:
                raise ValueError("J485 guard is not identity: " + row.path)
        else:
            mutations += 1
            if row.before_sha256 == row.after_sha256:
                raise ValueError("J485 mutation has no delta: " + row.path)

    if guards != expected_guards or mutations != expected_mutations:
        raise ValueError(
            f"J485 mode counts drifted: guards={guards}, mutations={mutations}"
        )
    return guards, mutations


def main(argv: list[str] | None = None) -> int:
    args = sys.argv[1:] if argv is None else argv
    repo = Path(args[0]) if args else Path(__file__).resolve().parents[4]
    guards, mutations = verify(repo)
    print(f"J485_CURRENT_TREE_PASS guards={guards} mutations={mutations}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
