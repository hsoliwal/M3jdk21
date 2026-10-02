#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checks for the JDK 22–27 downstream backport catalogue."""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path

import plan as backport_plan

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


def verify_work_queue(root: Path) -> None:
    expected = backport_plan.render(backport_plan.build(root))
    actual_path = root / "m3/backports/BACKPORT_WORK_QUEUE.tsv"
    actual = actual_path.read_text(encoding="utf-8")
    if actual != expected:
        raise AssertionError(
            "BACKPORT_WORK_QUEUE.tsv differs from deterministic regeneration"
        )

    rows = read_tsv(actual_path)
    if len(rows) != 95:
        raise AssertionError(f"expected 95 backport work rows, found {len(rows)}")
    identities = [row["identity"] for row in rows]
    if len(identities) != len(set(identities)):
        raise AssertionError("duplicate identity in backport work queue")


def verify_passes(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/BACKPORT_PASSES.tsv")
    if len(rows) != 7:
        raise AssertionError(f"expected 7 backport passes, found {len(rows)}")
    for ordinal, row in enumerate(rows):
        if int(row["ordinal"]) != ordinal:
            raise AssertionError(
                f"backport pass ordinal drift at {ordinal}: {row['ordinal']}"
            )
        if not row["pass_id"].strip() or not row["stop_condition"].strip():
            raise AssertionError(f"incomplete backport pass row: {row}")


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
    verify_work_queue(root)
    verify_passes(root)
    verify_jcmd_backport(root)
    print(
        "PASS: 82 JEP rows, 95 deterministic work items, 7 backport passes, "
        "non-JEP seed uniqueness, and exact JDK-8357439 donor blobs"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
