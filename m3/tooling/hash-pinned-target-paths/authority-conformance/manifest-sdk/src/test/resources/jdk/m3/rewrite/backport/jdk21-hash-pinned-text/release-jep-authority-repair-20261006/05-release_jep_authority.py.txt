#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed released JDK 22-27 feature-JEP denominator authority."""

from __future__ import annotations

import csv
from pathlib import Path

RELEASES = tuple(range(22, 28))
EXPECTED_TOTAL = 85


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise AssertionError(f"empty TSV: {path}")
    return rows


def parse_ids(value: str) -> tuple[int, ...]:
    ids = tuple(int(item) for item in value.split(",") if item)
    if not ids or ids != tuple(sorted(set(ids))):
        raise AssertionError(f"invalid canonical JEP list: {value}")
    return ids


def authority(root: Path) -> dict[int, tuple[int, ...]]:
    rows = read_tsv(root / "m3/backports/RELEASE_JEP_AUTHORITY.tsv")
    if len(rows) != len(RELEASES):
        raise AssertionError(
            f"expected {len(RELEASES)} release authority rows, found {len(rows)}"
        )

    result: dict[int, tuple[int, ...]] = {}
    for row in rows:
        release = int(row["release"])
        if release not in RELEASES or release in result:
            raise AssertionError(f"invalid/duplicate release authority: {release}")
        if row["status"] != "RELEASED":
            raise AssertionError(
                f"JDK {release} must be RELEASED in the current denominator: {row['status']}"
            )
        source = row["source"]
        if source != f"https://openjdk.org/projects/jdk/{release}/":
            raise AssertionError(
                f"unexpected OpenJDK release source for {release}: {source}"
            )
        if not row["reviewed_utc"]:
            raise AssertionError(f"missing authority review date for JDK {release}")
        result[release] = parse_ids(row["official_feature_jep_ids"])

    if tuple(sorted(result)) != RELEASES:
        raise AssertionError(f"release authority set mismatch: {sorted(result)}")
    total = sum(len(values) for values in result.values())
    if total != EXPECTED_TOTAL:
        raise AssertionError(
            f"released JEP authority expected {EXPECTED_TOTAL} rows, found {total}"
        )
    return result


def verify_repository_authority(root: Path) -> dict[int, tuple[int, ...]]:
    expected = authority(root)
    catalogue = read_tsv(root / "m3/backports/JEP_CATALOGUE.tsv")
    actual: dict[int, set[int]] = {release: set() for release in RELEASES}

    for row in catalogue:
        release = int(row["release"])
        jep = int(row["jep"])
        if release not in actual:
            raise AssertionError(f"catalogue contains unexpected JDK release: {release}")
        if jep in actual[release]:
            raise AssertionError(f"duplicate JEP {jep} in JDK {release}")
        actual[release].add(jep)

    for release, ids in expected.items():
        expected_set = set(ids)
        if actual[release] != expected_set:
            missing = sorted(expected_set - actual[release])
            extra = sorted(actual[release] - expected_set)
            raise AssertionError(
                f"JDK {release} catalogue authority mismatch "
                f"missing={missing} extra={extra}"
            )
    return expected
