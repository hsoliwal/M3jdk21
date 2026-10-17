#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Fail-closed source custody for M3 Collection SDK receipt."""
import hashlib
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = Path(__file__).with_name("SDK_SOURCE_MANIFEST.tsv")
EXPECTED = (
    "m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java",
    "m3/collections/src/main/java/com/m3/collections/M3IndexPartitions.java",
    "m3/collections/src/main/java/com/m3/collections/M3LongBag.java",
    "m3/collections/src/test/java/com/m3/collections/M3CollectionSdkJUnitTest.java",
    "m3/collections/src/test/java/com/m3/collections/M3CollectionSdkSelfTest.java",
)

def source_rows(manifest=MANIFEST):
    if manifest.is_symlink() or not manifest.is_file():
        raise ValueError("SDK source receipt missing or aliased")
    rows = [line.split("\t") for line in manifest.read_text().splitlines()
            if line and not line.startswith("#")]
    if len(rows) != len(EXPECTED):
        raise ValueError("SDK receipt source count drift")
    for fields, path in zip(rows, EXPECTED):
        if (len(fields) != 4 or fields[0] != path or fields[1] != "ABSENT"
                or len(fields[2]) != 64
                or any(c not in "0123456789abcdef" for c in fields[2])
                or fields[3] != path.rsplit("/", 1)[1] + ".after"):
            raise ValueError("Unapproved SDK source row")
    return rows

def verify(root=ROOT, manifest=MANIFEST):
    for path, _before, expected, _template in source_rows(manifest):
        source = root / path
        if source.is_symlink() or not source.is_file():
            raise ValueError("SDK source missing or aliased: " + path)
        if hashlib.sha256(source.read_bytes()).hexdigest() != expected:
            raise ValueError("SDK source drift: " + path)
    return len(EXPECTED)

if __name__ == "__main__":
    try:
        print("PASS M3 SDK sealed sources", verify())
    except ValueError as error:
        sys.exit(str(error))
