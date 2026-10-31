#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify original M3FS manager-bulk source custody without mutation."""
import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")

def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def verify():
    paths = []
    for row in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not row or row.startswith("#"):
            continue
        cells = row.split("\t")
        if len(cells) != 4:
            raise ValueError("bad source receipt columns")
        name, prior, expected, category = cells
        if (not name or name.startswith("/") or "\\" in name
                or any(p in ("", ".", "..") for p in name.split("/"))
                or not re.fullmatch(r"[0-9a-f]{40}", expected)
                or prior != "ABSENT" and not re.fullmatch(r"[0-9a-f]{40}", prior)
                or not category):
            raise ValueError("bad input path or source identity")
        candidate = ROOT / name
        if candidate.is_symlink() or not candidate.is_file():
            raise ValueError("missing or symlinked SDK source: " + name)
        if blob(candidate.read_bytes()) != expected:
            raise ValueError("Git source postimage mismatch: " + name)
        paths.append(name)
    if len(paths) != 5 or paths != sorted(paths) or len(set(paths)) != len(paths):
        raise ValueError("incomplete or duplicate M3FS provider receiver")
    print("M3FS_MANAGER_BULK_BLOB_PASS files=5")

if __name__ == "__main__":
    try:
        verify()
    except (OSError, ValueError) as problem:
        print("M3FS_MANAGER_BULK_BLOB_REFUSED " + str(problem), file=sys.stderr)
        sys.exit(1)
