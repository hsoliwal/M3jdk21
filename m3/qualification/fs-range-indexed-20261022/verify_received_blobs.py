#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail closed on any drift in the seven received SDK Java/test/verifier source files."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")
EXPECTED_COUNT = 7


def git_blob(content: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(content)).encode("ascii") + b"\x00" + content).hexdigest()


def verify() -> None:
    rows = []
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) != 6:
            raise ValueError("incorrect source receipt width")
        name, prior, candidate, donor, canonical, category = parts
        if (not name or name.startswith("/") or "\\" in name
                or any(p in ("", ".", "..") for p in name.split("/"))
                or not re.fullmatch(r"[0-9a-f]{40}", candidate)
                or prior != "ABSENT" and not re.fullmatch(r"[0-9a-f]{40}", prior)
                or not donor.isdecimal() or not canonical.isdecimal() or not category):
            raise ValueError("invalid source receipt: " + name)
        path = ROOT / name
        if path.is_symlink() or not path.is_file():
            raise ValueError("missing or linked original SDK source: " + name)
        actual = git_blob(path.read_bytes())
        if actual != candidate:
            raise ValueError("source drift: " + name + " actual=" + actual + " expected=" + candidate)
        rows.append(name)
    if len(rows) != EXPECTED_COUNT or len(set(rows)) != EXPECTED_COUNT or rows != sorted(rows):
        raise ValueError("incomplete, duplicated or unsorted source receipt")
    print("M3FS_RANGE_INDEXED_BLOB_PASS files=" + str(len(rows)))


if __name__ == "__main__":
    try:
        verify()
    except (OSError, ValueError) as failure:
        print("M3FS_RANGE_INDEXED_BLOB_REFUSED " + str(failure), file=sys.stderr)
        sys.exit(1)
