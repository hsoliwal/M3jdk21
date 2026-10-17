#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact three-owner source successor for indexed window mask operations.

Uses the pre-existing fixed-source/JNI mask receipts without replacing or
relaxing historical manifests. Does not write product files.
"""
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RECEIPT = HERE / "qualification/indexed-window-mask-20261017.tsv"
RECEIPT_SHA256 = "9307f19a9d7209b5c5f4cbd78cf2021573358730fbba71241670fcbdca9b54fd"
EXPECTED = (
    ("m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java",
     "8c1aaba72372c6caecfa35d4121a0b518888d64933bef428a8e61c15b3a746d9",
     "7bf07e036b2af4442834ddcd723c44584337610ed49e9084acb2cd4770152103"),
    ("m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
     "d7786cd5308f4a1e6743680d35e51f5f11a66b106abdb8f170edcb92b901d1cb",
     "028c5160b35475508ba92831782813f5491c28b56ebc62af2d0b773d7d0334e4"),
    ("m3/collections/src/test/java/com/m3/collections/M3IndexMaskCompactorSelfTest.java",
     "86eb2c9a436aac6f6e5b99f5c275636ca272d849dd08aa07f8796271167c63a1",
     "98f83664de9e1846f5309abaf0369e18fa0a5c2b2d16486b03f5c66647424237"),
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def approved_successor(receipt: Path = RECEIPT) -> dict[str, tuple[str, str]]:
    if receipt.is_symlink() or not receipt.is_file():
        raise ValueError("missing or aliased M3 offset-window receipt")
    if sha256(receipt) != RECEIPT_SHA256:
        raise ValueError("M3 offset-window receipt has changed")
    with receipt.open(encoding="utf-8", newline="") as handle:
        rows = [tuple(row) for row in csv.reader(handle, delimiter="\t", strict=True)]
    if rows != list(EXPECTED):
        raise ValueError("noncanonical or duplicate M3 offset-window receipt")
    return {path: (before, after) for path, before, after in rows}


def qualified_hash(path: str, previous_hash: str,
                   successor: dict[str, tuple[str, str]]) -> str:
    if path not in successor:
        return previous_hash
    before, after = successor[path]
    if previous_hash != before:
        raise ValueError("historical M3 source preimage mismatch: " + path)
    return after
