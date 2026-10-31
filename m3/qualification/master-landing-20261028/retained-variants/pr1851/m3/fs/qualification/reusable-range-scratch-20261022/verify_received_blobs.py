#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Strict, read-only Git-blob proof for two original M3FS Java21 owners."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[4]
LEDGER = Path(__file__).with_name("RECEIVED_DONORS.tsv")


def object_id(contents: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(contents)).encode("ascii")
                        + b"\x00" + contents).hexdigest()


def main() -> None:
    pairs = []
    for line in LEDGER.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 5:
            raise ValueError("invalid receiving ledger row")
        relative, before, after, recipe, role = cells
        if (relative.startswith("/") or "\\" in relative
                or any(part in ("", ".", "..") for part in relative.split("/"))
                or not re.fullmatch(r"[0-9a-f]{40}", before)
                or not re.fullmatch(r"[0-9a-f]{40}", after)
                or not recipe or not role):
            raise ValueError("invalid M3FS source receipt")
        file = ROOT / relative
        if file.is_symlink() or not file.is_file() or object_id(file.read_bytes()) != after:
            raise ValueError("source drift: " + relative)
        pairs.append(relative)
    if pairs != sorted(set(pairs)) or len(pairs) != 2:
        raise ValueError("duplicate or missing source owners")
    print("M3FS_REUSABLE_RANGE_SCRATCH_BLOB_PASS files=" + str(len(pairs)))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as error:
        print("M3FS_REUSABLE_RANGE_SCRATCH_REFUSED " + str(error), file=sys.stderr)
        sys.exit(1)
