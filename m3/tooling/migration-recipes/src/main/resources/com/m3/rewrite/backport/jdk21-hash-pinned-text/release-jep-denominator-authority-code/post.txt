#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Authoritative JDK release -> JEP denominator checks for M3JDK21."""

from __future__ import annotations

import csv
from pathlib import Path

RELEASES = tuple(range(22, 28))
RELEASED = {22, 23, 24, 25, 26}
SNAPSHOT = {27}


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
        raise AssertionError(f"expected {len(RELEASES)} release authority rows, found {len(rows)}")

    result: dict[int, tuple[int, ...]] = {}
    for row in rows:
        release = int(row["release"])
        if release not in RELEASES or release in result:
            raise AssertionError(f"invalid/duplicate release authority: {release}")
        expected_status = "RELEASED" if release in RELEASED else "IN_DEVELOPMENT_SNAPSHOT"
        if row["status"] != expected_status:
            raise AssertionError(
                f"release {release} status {row['status']} != {expected_status}"
            )
        source = row["source"]
        if source != f"https://openjdk.org/projects/jdk/{release}/":
            raise AssertionError(f"unexpected OpenJDK release source for {release}: {source}")
        if not row["reviewed_utc"]:
            raise AssertionError(f"missing authority review date for JDK {release}")
        result[release] = parse_ids(row["official_feature_jep_ids"])

    if tuple(sorted(result)) != RELEASES:
        raise AssertionError(f"release authority set mismatch: {sorted(result)}")
    return result


def verify_repository_authority(root: Path) -> dict[int, tuple[int, ...]]:
    expected = authority(root)
    catalog = read_tsv(root / "m3/backports/JEP_CATALOGUE.tsv")
    queue = read_tsv(root / "m3/backports/BACKPORT_WORK_QUEUE.tsv")

    catalog_by_release: dict[int, set[int]] = {release: set() for release in RELEASES}
    for row in catalog:
        release = int(row["release"])
        if release not in catalog_by_release:
            raise AssertionError(f"catalogue contains unexpected JDK release: {release}")
        jep = int(row["jep"])
        if jep in catalog_by_release[release]:
            raise AssertionError(f"duplicate JEP {jep} in release {release}")
        catalog_by_release[release].add(jep)

    queue_by_release: dict[int, set[int]] = {release: set() for release in RELEASES}
    for row in queue:
        if row["source_type"] != "JEP":
            continue
        release = int(row["release"])
        identity = row["identity"]
        if release not in queue_by_release or not identity.startswith("JEP-"):
            raise AssertionError(f"invalid queued JEP row: {row}")
        queue_by_release[release].add(int(identity.removeprefix("JEP-")))

    for release, ids in expected.items():
        expected_set = set(ids)
        if catalog_by_release[release] != expected_set:
            missing = sorted(expected_set - catalog_by_release[release])
            extra = sorted(catalog_by_release[release] - expected_set)
            raise AssertionError(
                f"JDK {release} catalogue authority mismatch missing={missing} extra={extra}"
            )
        if queue_by_release[release] != expected_set:
            missing = sorted(expected_set - queue_by_release[release])
            extra = sorted(queue_by_release[release] - expected_set)
            raise AssertionError(
                f"JDK {release} queue authority mismatch missing={missing} extra={extra}"
            )
    return expected
