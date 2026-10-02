#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checks for the JDK 22–27 downstream backport catalogue."""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path

EXPECTED_RELEASE_COUNTS = {22: 12, 23: 12, 24: 22, 25: 17, 26: 10, 27: 9}
EXPECTED_TOTAL = sum(EXPECTED_RELEASE_COUNTS.values())


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise AssertionError(f"empty TSV: {path}")
    return rows


def verify_jeps(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/JEP_CATALOGUE.tsv")
    if len(rows) != EXPECTED_TOTAL:
        raise AssertionError(f"expected {EXPECTED_TOTAL} JEP rows, found {len(rows)}")

    seen: set[int] = set()
    counts = {release: 0 for release in EXPECTED_RELEASE_COUNTS}
    catalog_numbers: set[int] = set()

    for row in rows:
        release = int(row["release"])
        jep = int(row["jep"])
        if release not in counts:
            raise AssertionError(f"unexpected release {release} for JEP {jep}")
        if jep in seen:
            raise AssertionError(f"duplicate JEP {jep}")
        seen.add(jep)
        catalog_numbers.add(jep)
        counts[release] += 1

        if row["domain"] == "language" and row["disposition"] != "reject-language":
            raise AssertionError(f"language JEP {jep} is not reject-language")

    if counts != EXPECTED_RELEASE_COUNTS:
        raise AssertionError(f"release counts differ: {counts}")

    for row in rows:
        superseded = row["superseded_by"].strip()
        if superseded and int(superseded) not in catalog_numbers:
            raise AssertionError(
                f"JEP {row['jep']} superseded_by {superseded} is outside catalogue"
            )


def verify_seed(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    ids = [row["jbs"] for row in rows]
    if len(ids) != len(set(ids)):
        raise AssertionError("duplicate JBS ID in upstream change seeds")
    for row in rows:
        if not row["jbs"].startswith("JDK-"):
            raise AssertionError(f"invalid JBS ID: {row['jbs']}")


def verify_jcmd_backport(root: Path) -> None:
    manifest = read_tsv(root / "m3/backports/recipes/jdk-8357439/manifest.tsv")
    if len(manifest) != 2:
        raise AssertionError(f"expected 2 jcmd manifest rows, found {len(manifest)}")
    for row in manifest:
        if row["preimage"] != "ABSENT":
            raise AssertionError(f"unexpected preimage state for {row['path']}")
        path = root / row["path"]
        if not path.is_file():
            raise AssertionError(f"missing backport target: {row['path']}")
        actual = git_blob_sha1(path.read_bytes())
        expected = row["upstream_git_blob"]
        if actual != expected:
            raise AssertionError(
                f"blob drift for {row['path']}: expected {expected}, actual {actual}"
            )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="repository root",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    verify_jeps(root)
    verify_seed(root)
    verify_jcmd_backport(root)
    print(
        "PASS: 82 JEP rows, non-JEP seed uniqueness, "
        "and exact JDK-8357439 donor blobs"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
