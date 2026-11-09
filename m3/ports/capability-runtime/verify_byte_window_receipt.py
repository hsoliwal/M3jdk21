#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed target proof: compare exact received source SHA-256 to canonical recipe."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RECEIPT = ROOT / "m3/ports/capability-runtime/recipe-receipts/CPU_BYTE_WINDOW_20261018.tsv"
PREFIX = "m3/ports/capability-runtime/src/"
OWNER = "Synexia:M3CpuByteWindowRecipe"


def main() -> None:
    rows = [
        row for row in RECEIPT.read_text(encoding="utf-8").splitlines()
        if row and not row.startswith("#") and not row.startswith("path\t")
    ]
    if len(rows) != 2:
        raise SystemExit("invalid exact source admission count")
    paths = []
    for row in rows:
        values = row.split("\t")
        if len(values) != 4:
            raise SystemExit("invalid source receipt")
        name, expected, owner, disposition = values
        if (not name.startswith(PREFIX) or not name.endswith(".java")
                or name.startswith("/") or ".." in Path(name).parts
                or owner != OWNER or disposition != "CANDIDATE_UNQUALIFIED"
                or len(expected) != 64):
            raise SystemExit("invalid pinned target")
        paths.append(name)
        actual = hashlib.sha256((ROOT / name).read_bytes()).hexdigest()
        if expected != actual:
            raise SystemExit("M3 CPU source drift: " + name)
    if paths != sorted(paths) or len(set(paths)) != 2:
        raise SystemExit("unordered or repeated source owner")
    print("M3_CPU_WINDOW_SOURCE_RECEIPT_PASS targets=2")


if __name__ == "__main__":
    main()
