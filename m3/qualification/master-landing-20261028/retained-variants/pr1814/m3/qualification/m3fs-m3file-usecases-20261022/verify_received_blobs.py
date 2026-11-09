#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only source-custody check of original M3FS/M3File Git blob postimages."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")
EXPECTED = 22


def git_blob(body: bytes) -> str:
    prefix = b"blob " + str(len(body)).encode("ascii") + b"\0"
    return hashlib.sha1(prefix + body).hexdigest()


def main() -> None:
    seen: set[str] = set()
    previous = ""
    count = 0
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        columns = line.split("\t")
        if len(columns) != 6:
            raise ValueError("invalid source ledger row")
        path, before, after, donor, recipe, category = columns
        if (not path or path <= previous or path.startswith("/") or "\\" in path
                or any(segment in ("", ".", "..") for segment in path.split("/"))
                or path in seen or not category or not donor or not recipe
                or not re.fullmatch(r"[0-9a-f]{40}", after)
                or before != "ABSENT" and not re.fullmatch(r"[0-9a-f]{40}", before)):
            raise ValueError("untrusted source path/receipt: " + path)
        original = ROOT / path
        if original.is_symlink() or not original.is_file():
            raise ValueError("missing or linked source: " + path)
        actual = git_blob(original.read_bytes())
        if actual != after:
            raise ValueError("source drift: " + path + " got " + actual)
        previous = path
        seen.add(path)
        count += 1
    if count != EXPECTED:
        raise ValueError("incomplete source receipt; expected " + str(EXPECTED))
    print("M3_FS_FILE_USE_CASES_BLOB_PASS files=" + str(count))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as failure:
        print("M3_FS_FILE_USE_CASES_BLOB_REFUSED " + str(failure), file=sys.stderr)
        sys.exit(1)
