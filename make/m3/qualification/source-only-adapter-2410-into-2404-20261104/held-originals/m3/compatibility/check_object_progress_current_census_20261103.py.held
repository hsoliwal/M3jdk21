#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only, observed-only census of current Object/progress receiver files.

A matching current-file census is NOT a substitute for the historical
check_object_adapter_progress_receiver_20261017.py admission gate.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
from pathlib import Path, PurePosixPath
import re

import check_object_adapter_progress_receiver_20261017 as historical

ROOT = Path(__file__).resolve().parents[2]
CENSUS = "m3/compatibility/m3-object-progress-current-census-20261103.tsv"
CENSUS_SHA256 = "f333de2f423f079f3f28b49e4c531548aa29511af30227dd84c10efa7b7a48cf"
HEADER = "# path\thistorical_sha256\tobserved_sha256\tobserved_git_blob_sha1"
ROWS = 14
DIVERGENCES = 8
HEX64 = re.compile(r"[0-9a-f]{64}\Z")
HEX40 = re.compile(r"[0-9a-f]{40}\Z")


@dataclass(frozen=True)
class CensusRow:
    path: str
    historical_sha256: str
    observed_sha256: str
    observed_git_blob_sha1: str


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse_rows(data: bytes, expected_sha256: str) -> tuple[CensusRow, ...]:
    """Require a complete, exact UTF-8, sorted and checksum-pinned census."""
    if digest(data) != expected_sha256:
        raise RuntimeError("source census SHA-256 drift")
    if not data.endswith(b"\n") or b"\r" in data or b"\0" in data:
        raise RuntimeError("invalid census encoding")
    text = data.decode("utf-8", errors="strict")
    lines = text.splitlines()
    if len(lines) != ROWS + 1 or lines[0] != HEADER:
        raise RuntimeError("source census row coverage or header drift")
    records: list[CensusRow] = []
    previous = ""
    for line in lines[1:]:
        cells = line.split("\t")
        if len(cells) != 4:
            raise RuntimeError("invalid source census row")
        path, old, current, blob = cells
        posix = PurePosixPath(path)
        if (not path.startswith(("src/java.base/share/classes/", "test/jdk/"))
                or str(posix) != path or posix.is_absolute() or ".." in posix.parts
                or "\\" in path or path <= previous
                or not HEX64.fullmatch(old) or not HEX64.fullmatch(current)
                or not HEX40.fullmatch(blob)):
            raise RuntimeError("invalid source census path, digest or ordering: " + path)
        records.append(CensusRow(path, old, current, blob))
        previous = path
    return tuple(records)


def audit(rows: tuple[CensusRow, ...], historical_hashes: dict[str, str],
          read_source) -> tuple[int, int, tuple[str, ...]]:
    """Verify observed bytes, without granting old-to-current equivalence."""
    names = {record.path for record in rows}
    if len(rows) != ROWS or names != set(historical_hashes):
        raise RuntimeError("historical and census closed sets differ")
    same = 0
    divergent: list[str] = []
    for record in rows:
        if historical_hashes[record.path] != record.historical_sha256:
            raise RuntimeError("historical source transition drift: " + record.path)
        actual = digest(read_source(record.path))
        if actual != record.observed_sha256:
            raise RuntimeError("observed source drift: " + record.path
                               + ": " + actual + " != " + record.observed_sha256)
        if record.historical_sha256 == record.observed_sha256:
            same += 1
        else:
            divergent.append(record.path)
    if len(divergent) != DIVERGENCES or same != ROWS - DIVERGENCES:
        raise RuntimeError("unexpected historical divergence count")
    return same, len(divergent), tuple(divergent)


def verify(root: Path = ROOT) -> int:
    """Use only the original hash-sealed manifest composer and safe reader."""
    records = parse_rows(historical.contained_file(root, CENSUS).read_bytes(), CENSUS_SHA256)
    expected = historical.current_targets(root)
    same, different, paths = audit(records, expected,
        lambda path: historical.contained_file(root, path).read_bytes())
    for path in paths:
        print("HISTORICAL_DIVERGENCE " + path)
    print(f"OBSERVED_ONLY {len(records)} exact current sources: "
          f"{same} historical matches, {different} unadmitted transitions; "
          "historical release gate remains authoritative")
    return len(records)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT, help="read-only checkout")
    args = parser.parse_args()
    verify(args.root)


if __name__ == "__main__":
    main()
