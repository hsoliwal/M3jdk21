#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact chained source proof for four parent-owned 64-bit object-state atoms.

The first (byte-mask/offset) successor is immutable. This second successor
must match its known postimage or the historical baseline exactly before
admitting 64-bit state words. No generated product output is written.
"""
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RECEIPT = HERE / "qualification/slot-state-word-20261017.tsv"
RECEIPT_SHA256 = "260f256aafe1ae4fd2e36f1c2c30e2eff7839e882cd9874a9a7af302ef34262d"
EXPECTED = (
    ("m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java",
     "7bf07e036b2af4442834ddcd723c44584337610ed49e9084acb2cd4770152103",
     "f84e7e21f44286b5b8fa423b192ef493486a7b5ffbaa9ce01d99a72d859aab91"),
    ("m3/collections/src/main/java/com/m3/collections/M3ObjectResidency.java",
     "c8ffc48d43e98223ae37e722c5841b962217fb6b98daf561cba430eb3492bdc5",
     "4033816e5c7dfa635289f9978a17963e177aa8073281bae82dbe698190f3b412"),
    ("m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
     "028c5160b35475508ba92831782813f5491c28b56ebc62af2d0b773d7d0334e4",
     "7412d39345577127d2c1cdbc34bad1992bf40bb806a0fc5e5ddb60174f81bc66"),
    ("m3/collections/src/test/java/com/m3/collections/M3IndexMaskCompactorSelfTest.java",
     "98f83664de9e1846f5309abaf0369e18fa0a5c2b2d16486b03f5c66647424237",
     "f5e2df6b2b035d4293f20f33dd72791a6b1bdc3639ac3a63b83f8a2b29ce5937"),
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def approved_successor(receipt: Path = RECEIPT) -> dict[str, tuple[str, str]]:
    if receipt.is_symlink() or not receipt.is_file():
        raise ValueError("missing or aliased slot-state successor")
    if sha256(receipt) != RECEIPT_SHA256:
        raise ValueError("state-word successor receipt drift")
    with receipt.open(encoding="utf-8", newline="") as handle:
        rows = [tuple(row) for row in csv.reader(handle, delimiter="\t", strict=True)]
    if rows != list(EXPECTED):
        raise ValueError("invalid/duplicate/reordered state-word successor")
    return {name: (old, new) for name, old, new in rows}


def qualified_hash(path: str, prior: str, accepted: dict[str, tuple[str, str]]) -> str:
    transition = accepted.get(path)
    if transition is None:
        return prior
    before, after = transition
    if prior != before:
        raise ValueError("state-word successor predecessor mismatch: " + path)
    return after
