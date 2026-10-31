#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify received original Git blobs offline, without modifying a source or running a parser."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")
EXPECTED_ROWS = 20


def git_blob_id(contents: bytes) -> str:
    header = b"blob " + str(len(contents)).encode("ascii") + b"\x00"
    return hashlib.sha1(header + contents).hexdigest()


def verify() -> int:
    rows = []
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 5 or not re.fullmatch(r"[0-9a-f]{40}", cells[2]):
            raise ValueError("invalid Git blob manifest row: " + line[:150])
        name, origin, digest, role, custody = cells
        if (not name or name.startswith("/") or "\\" in name
                or any(part in ("", ".", "..") for part in name.split("/"))
                or not origin or not role
                or custody != "GITHUB_POSTIMAGE_READBACK_MATCH"):
            raise ValueError("unsafe source provenance: " + name)
        rows.append((name, digest))
    if len(rows) != EXPECTED_ROWS or rows != sorted(rows) or len(set(rows)) != len(rows):
        raise ValueError("missing, unsorted or duplicate receiving manifest rows")
    for name, expected in rows:
        path = ROOT / name
        if path.is_symlink() or not path.is_file():
            raise ValueError("not a regular source file: " + name)
        actual = git_blob_id(path.read_bytes())
        if actual != expected:
            raise ValueError("source drift: " + name + " expected " + expected + " got " + actual)
    print("M3_SUPERSET_BLOB_PASS files=" + str(len(rows)))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(verify())
    except (OSError, ValueError) as error:
        print("M3_SUPERSET_BLOB_REFUSED " + str(error), file=sys.stderr)
        sys.exit(1)
