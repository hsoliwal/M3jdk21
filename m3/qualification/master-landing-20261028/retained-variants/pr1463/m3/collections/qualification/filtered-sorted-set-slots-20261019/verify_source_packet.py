#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receipt for original M3JDK21 sorted-set slot owner source."""
from __future__ import annotations

import hashlib
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
MANIFEST = pathlib.Path(__file__).resolve().parent / "PRODUCT_SOURCE_MANIFEST.tsv"
HASH = re.compile(r"[0-9a-f]{64}\Z")
PREFIX = "m3/collections/"
EXPECTED = (
    "src/main/java/com/m3/collections/M3TreeSet.java",
    "src/main/java/com/m3/collections/M3FilteredViews.java",
    "src/main/java/com/m3/collections/M3MutableFilteredViews.java",
    "src/test/java/com/m3/collections/M3FilteredSortedSetSlotSelfTest.java",
    "src/test/java/com/m3/collections/M3FilteredSortedSetSlotTest.java",
)


def receipt_bytes() -> list[tuple[str, str, str, str]]:
    rows: list[tuple[str, str, str, str]] = []
    for number, line in enumerate(MANIFEST.read_text(encoding="utf-8").splitlines(), 1):
        columns = line.split("\t")
        if len(columns) != 4:
            raise ValueError(f"manifest row {number}: expected 4 tab-separated columns")
        target, before, after, resource = columns
        if not target.startswith(PREFIX) or target.removeprefix(PREFIX) not in EXPECTED:
            raise ValueError(f"manifest row {number}: unexpected target {target!r}")
        if (before != "ABSENT" and not HASH.fullmatch(before)) or not HASH.fullmatch(after):
            raise ValueError(f"manifest row {number}: malformed SHA256")
        if resource != pathlib.PurePosixPath(target).name + ".txt":
            raise ValueError(f"manifest row {number}: wrong postimage resource name")
        if "SelfTest.java" in target or target.endswith("SlotTest.java"):
            if before != "ABSENT":
                raise ValueError(f"manifest row {number}: new test preimage must be ABSENT")
        elif before == "ABSENT":
            raise ValueError(f"manifest row {number}: original owner preimage cannot be ABSENT")
        rows.append((target, before, after, resource))
    actual = [row[0].removeprefix(PREFIX) for row in rows]
    if actual != list(EXPECTED):
        raise ValueError("missing, duplicated, added or reordered set-view source packet")
    return rows


def verify_data(rows: list[tuple[str, str, str, str]], entries: dict[str, bytes]) -> None:
    for target, _, after, _ in rows:
        raw = entries.get(target)
        if raw is None:
            raise ValueError(f"missing M3 product source {target}")
        if hashlib.sha256(raw).hexdigest() != after:
            raise ValueError(f"unexpected M3 product owner content: {target}")


def main() -> None:
    rows = receipt_bytes()
    entries = {target: (ROOT / target).read_bytes() for target, _, _, _ in rows}
    verify_data(rows, entries)
    print(f"M3_DOOP_SET_SOURCE_SEAL_PASS rows={len(rows)}")

    if len(sys.argv) == 2 and sys.argv[1] == "--self-test":
        # The same exact verifier must reject one tampered source byte, a
        # different target path, and duplicate or removed manifest entries.
        original = rows[0][0]
        corrupted = dict(entries)
        corrupted[original] = entries[original] + b"\n// tampering\n"
        try:
            verify_data(rows, corrupted)
        except ValueError:
            pass
        else:
            raise AssertionError("source-tamper refusal failed")
        try:
            verify_data(rows, {k: v for k, v in entries.items() if k != original})
        except ValueError:
            pass
        else:
            raise AssertionError("missing-file refusal failed")
        print("M3_DOOP_SET_SOURCE_SEAL_REFUSALS_PASS cases=2")
    elif len(sys.argv) != 1:
        raise SystemExit("usage: verify_source_packet.py [--self-test]")


if __name__ == "__main__":
    main()
