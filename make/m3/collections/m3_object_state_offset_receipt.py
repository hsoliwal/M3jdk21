#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only successor for two generation-safe per-object mask/offset owners.

An original donor byte SHA-256 is required; this never grants arbitrary
source-file additions or changes, and never mutates JDK product source.
"""
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RECEIPT = HERE / "qualification/object-state-offset-jni-20261017.tsv"
RECEIPT_SHA256 = "de3504abbbefb2c67cf94c0ad61974ffbce3339ac4f4f530443e1af82635f9a4"
EXPECTED = (
    ("m3/collections/src/main/java/jdk/m3/collections/M3ObjectStateSlots28.java", "8e1f38cace3a7e96e94707e06a25fc8fe2bf4a7315ac0ed11de1e8249a3e3c88", "c8c8b273da8d20ace83148230c62f63e03f92efa2cf880f935f9fae8dcdd3bc2"),
    ("m3/collections/src/test/java/jdk/m3/collections/M3ObjectStateSlots28SelfTest.java", "e28893fb6734437cee712554f350fa173a3189a500feb64c80c678ca046e2f7b", "450df1026701f89ed870536e9284c766a3bd99409c0ac54d5f22462c2da4ca89"),
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def approved_successor(receipt: Path = RECEIPT) -> dict[str, tuple[str, str]]:
    if receipt.is_symlink() or not receipt.is_file():
        raise ValueError("missing or aliased object-state successor")
    if sha256(receipt) != RECEIPT_SHA256:
        raise ValueError("object-state receipt SHA-256 drift")
    with receipt.open(encoding="utf-8", newline="") as stream:
        rows = [tuple(row) for row in csv.reader(stream, delimiter="\t", strict=True)]
    if rows != list(EXPECTED):
        raise ValueError("duplicate/noncanonical object-state successor")
    return {path: (old, new) for path, old, new in rows}


def include_successor(historical_rows, successor=None):
    """Retain all historical rows; introduce/replace only the two exact owners."""
    approved = approved_successor() if successor is None else successor
    if set(approved) != {row[0] for row in EXPECTED}:
        raise ValueError("unapproved object-state entry")
    result = list(historical_rows)
    seen = set()
    for i, row in enumerate(result):
        if len(row) != 2:
            raise ValueError("malformed historical source row")
        path, old = row
        if path in seen:
            raise ValueError("duplicate historical source path")
        seen.add(path)
        if path in approved:
            required_old, new = approved[path]
            if old != required_old:
                raise ValueError("object-state predecessor drift: " + path)
            result[i] = (path, new)
    for path, (required_old, new) in approved.items():
        if path not in seen:
            result.append((path, new))
    return result
