#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Offline exact Git blob custody of the original Java/C/M3 collection receiver."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")
COUNT = 12


def blob(contents: bytes) -> str:
    header = b"blob " + str(len(contents)).encode("ascii") + b"\x00"
    return hashlib.sha1(header + contents).hexdigest()


def verify() -> None:
    seen: set[str] = set()
    paths: list[str] = []
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 6:
            raise ValueError("invalid six-column source receipt")
        relative, before, after, donor, recipe, category = cells
        if (not relative or relative.startswith("/") or "\\" in relative
                or any(p in ("", ".", "..") for p in relative.split("/"))
                or relative in seen or not donor.isdecimal() or not recipe.isdecimal()
                or not category or not re.fullmatch("[0-9a-f]{40}", after)
                or before != "ABSENT" and not re.fullmatch("[0-9a-f]{40}", before)):
            raise ValueError("invalid or duplicate source identity " + relative)
        seen.add(relative)
        paths.append(relative)
        path = ROOT / relative
        if path.is_symlink() or not path.is_file():
            raise ValueError("missing or linked original source " + relative)
        actual = blob(path.read_bytes())
        if actual != after:
            raise ValueError("source drift: " + relative + " " + actual + " expected " + after)
    if len(paths) != COUNT or paths != sorted(paths):
        raise ValueError("incomplete or unsorted twelve-file receiving ledger")
    print("M3_NATIVE_FILTERED_SUPERSET_BLOB_PASS files=" + str(len(paths)))


if __name__ == "__main__":
    try:
        verify()
    except (OSError, ValueError) as error:
        print("M3_NATIVE_FILTERED_SUPERSET_REFUSED " + str(error), file=sys.stderr)
        sys.exit(1)
