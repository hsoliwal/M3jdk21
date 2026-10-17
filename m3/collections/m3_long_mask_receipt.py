#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-sealed, read-only custody for exactly three native64 mask successors.

The historical installed Java inventory and JNI receipt remain immutable.
Approval applies only to exact old -> new source bytes; no source mutation,
JNI promotion or fallback-to-unpinned-code is authorized.
"""
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
RECEIPT = HERE / "qualification/long-mask-jni-successor-20261017.tsv"
RECEIPT_SHA256 = "73d6521fd92b9d093059f45e39fc28906ca7fbbbafd651e871ec931a1981547f"
EXPECTED = (
    ('m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java','8c1aaba72372c6caecfa35d4121a0b518888d64933bef428a8e61c15b3a746d9','7a53942a472639b4285438b3c5cf2fba63b91284d7f8cd3b2f3f6bbebf8be3db'),
    ('m3/collections/src/main/native/collections/m3_index_mask_compactor.c','d7786cd5308f4a1e6743680d35e51f5f11a66b106abdb8f170edcb92b901d1cb','0c25d038ef4c6fb552be05c617ccffdc241d65839452b4041891d447bd0b8c08'),
    ('m3/collections/src/test/java/com/m3/collections/M3IndexMaskCompactorSelfTest.java','86eb2c9a436aac6f6e5b99f5c275636ca272d849dd08aa07f8796271167c63a1','e06bee6330e65bd91f3fbcc110c86dba6aa7db544b7ca0005e479327fe2edf34'),
)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def approved_successor(receipt: Path = RECEIPT) -> dict[str, tuple[str, str]]:
    """Never trust a relocated, symlinked, malformed or stale replacement."""
    if receipt.is_symlink() or not receipt.is_file():
        raise ValueError("missing or aliased long-mask successor receipt")
    if digest(receipt) != RECEIPT_SHA256:
        raise ValueError("long-mask successor receipt SHA-256 drift")
    with receipt.open(encoding="utf-8", newline="") as stream:
        rows = [tuple(row) for row in csv.reader(stream, delimiter="\t", strict=True)]
    if rows != list(EXPECTED):
        raise ValueError("unexpected long-mask successor paths or hashes")
    return {path: (before, after) for path, before, after in rows}


def effective_hash(path: str, historical_sha: str,
                   approved: dict[str, tuple[str, str]]) -> str:
    """Substitute only a recipe-proven successor of that exact historical SHA."""
    previous = approved.get(path)
    if previous is None:
        return historical_sha
    before, after = previous
    if historical_sha != before:
        raise ValueError("long-mask predecessor source has drifted: " + path)
    return after
