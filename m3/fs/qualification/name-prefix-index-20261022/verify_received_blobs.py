#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify three exact original-owner Git blobs without modifying source."""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[4]
MANIFEST = Path(__file__).with_name("RECEIVED_DONORS.tsv")


def source_blob(content: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(content)).encode("ascii") + b"\x00" + content).hexdigest()


def verify() -> None:
    names = []
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 5:
            raise ValueError("invalid original source receipt")
        name, before, after, recipe, role = fields
        if (not name or name.startswith("/") or "\\" in name
                or any(part in ("", ".", "..") for part in name.split("/"))
                or before != "ABSENT" and not re.fullmatch("[0-9a-f]{40}", before)
                or not re.fullmatch("[0-9a-f]{40}", after)
                or not recipe or not role):
            raise ValueError("unsafe source row " + name)
        source = ROOT / name
        if source.is_symlink() or not source.is_file() or source_blob(source.read_bytes()) != after:
            raise ValueError("source does not match qualified recipe output: " + name)
        names.append(name)
    if len(names) != 3 or len(set(names)) != 3 or names != sorted(names):
        raise ValueError("missing, unordered or duplicate receipt rows")
    print("M3FS_NAME_INDEX_RECEIVER_PASS files=3")


if __name__ == "__main__":
    try:
        verify()
    except (OSError, ValueError) as error:
        print("M3FS_NAME_INDEX_RECEIVER_REFUSED " + str(error), file=sys.stderr)
        sys.exit(1)
