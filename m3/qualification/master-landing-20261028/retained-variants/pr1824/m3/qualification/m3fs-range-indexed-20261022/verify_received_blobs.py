#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify exact source/postimage Git blobs without reparsing or mutating source."""
import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")
EXPECTED = 7

def git_blob(contents: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(contents)).encode("ascii") +
                        b"\x00" + contents).hexdigest()

def verify() -> int:
    lines = MANIFEST.read_text(encoding="utf-8").splitlines()
    rows = []
    for line in lines:
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 6:
            raise ValueError("invalid six-column source receipt")
        path, pre, post, donor, recipe, category = cells
        if (path.startswith("/") or "\\" in path
                or any(part in ("", ".", "..") for part in path.split("/"))
                or not re.fullmatch(r"[0-9a-f]{40}", post)
                or pre != "ABSENT" and not re.fullmatch(r"[0-9a-f]{40}", pre)
                or not donor.isdecimal() or not recipe.isdecimal() or not category):
            raise ValueError("malformed receipt " + path)
        rows.append((path, post))
    paths = [p for p, _ in rows]
    if len(rows) != EXPECTED or paths != sorted(paths) or len(set(paths)) != EXPECTED:
        raise ValueError("source receipt is incomplete, unsorted or duplicated")
    for name, expected in rows:
        path = ROOT / name
        if path.is_symlink() or not path.is_file():
            raise ValueError("original source is missing or linked: " + name)
        actual = git_blob(path.read_bytes())
        if actual != expected:
            raise ValueError("source postimage drift: " + name +
                             " got " + actual + " expected " + expected)
    print("M3FS_RANGE_INDEXED_BLOB_PASS files=" + str(len(rows)))
    return 0

if __name__ == "__main__":
    try:
        sys.exit(verify())
    except (OSError, ValueError) as e:
        print("M3FS_RANGE_INDEXED_BLOB_REFUSED " + str(e), file=sys.stderr)
        sys.exit(1)
