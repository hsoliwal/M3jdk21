#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Immutable upstream donor refs for M3JDK21 release/JEP planning."""

from __future__ import annotations

import csv
import re
from pathlib import Path

RELEASES = tuple(range(22, 28))
RELEASED = {22, 23, 24, 25, 26}
SNAPSHOT = {27}


def read(path: Path) -> dict[int, str]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != len(RELEASES):
        raise AssertionError(f"expected {len(RELEASES)} donor rows, found {len(rows)}")

    result: dict[int, str] = {}
    for row in rows:
        release = int(row["release"])
        if release not in RELEASES or release in result:
            raise AssertionError(f"invalid/duplicate release donor ref: {release}")
        expected_status = "RELEASED" if release in RELEASED else "IN_DEVELOPMENT_SNAPSHOT"
        if row["status"] != expected_status:
            raise AssertionError(
                f"release {release} donor status {row['status']} != {expected_status}"
            )
        ref = row["donor_ref"]
        kind = row["ref_kind"]
        if release in RELEASED:
            expected_ref = {
                22: "jdk-22+36",
                23: "jdk-23+37",
                24: "jdk-24+36",
                25: "jdk-25+36",
                26: "jdk-26+35",
            }[release]
            if kind != "TAG" or ref != expected_ref:
                raise AssertionError(
                    f"released donor {release} must use exact GA tag {expected_ref}, found {kind}:{ref}"
                )
        else:
            if kind != "COMMIT" or not re.fullmatch(r"[0-9a-f]{40}", ref):
                raise AssertionError(
                    "in-development JDK 27 donor must be an immutable 40-hex commit"
                )
        if not row["reviewed_utc"]:
            raise AssertionError(f"missing donor review date for JDK {release}")
        result[release] = ref

    if tuple(sorted(result)) != RELEASES:
        raise AssertionError(f"donor release set mismatch: {sorted(result)}")
    return result


def verify_with_release_authority(root: Path) -> dict[int, str]:
    import release_jep_authority

    release_jep_authority.authority(root)
    refs = read(root / "m3/backports/RELEASE_DONOR_REFS.tsv")
    if set(refs) != set(release_jep_authority.RELEASES):
        raise AssertionError("release-JEP authority and donor-ref authority release sets differ")
    return refs
