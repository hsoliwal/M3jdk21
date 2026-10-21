#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed SDK recovery admission; historical installed manifests remain immutable."""
import hashlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
RECEIPT = HERE / "RECEIPT.tsv"
RECEIPT_SHA256 = "b87d65b4588f8838b9d359c7e9267920db94b05aab9df878c18db92be4828af6"
TARGETS = (
    "m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java",
    "m3/collections/src/main/java/com/m3/collections/M3IndexPartitions.java",
    "m3/collections/src/main/java/com/m3/collections/M3LongBag.java",
    "m3/collections/src/test/java/com/m3/collections/M3SdkRecoverySelfTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3SdkRecoveryTest.java",
)
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def verify(root=ROOT, receipt=RECEIPT):
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != RECEIPT_SHA256:
        raise ValueError("missing, aliased, or modified SDK recovery receipt")
    rows = [line.split("\t") for line in receipt.read_text(encoding="utf-8").splitlines()]
    if len(rows) != len(TARGETS):
        raise ValueError("unexpected recovery target count")
    for path, cells in zip(TARGETS, rows):
        if len(cells) != 4 or cells[0] != path or cells[1] != (
                "ABSENT" if path != TARGETS[0] else
                "9a51f5edad8fd58ec9a7cc4dcd17cec23117de1eba486ba1b7a4c631acd8eff0"):
            raise ValueError("unexpected target/preimage: " + path)
        after, template = cells[2:]
        if len(after) != 64 or any(c not in "0123456789abcdef" for c in after) \
                or template != Path(path).name + ".txt":
            raise ValueError("bad SDK recovery receipt row: " + path)
        target = root / path
        if target.is_symlink() or not target.is_file() or digest(target) != after:
            raise ValueError("missing, aliased, or drifted SDK file: " + path)
    return len(TARGETS)

if __name__ == "__main__":
    print("PASS SDK recovery SHA-256", verify())
