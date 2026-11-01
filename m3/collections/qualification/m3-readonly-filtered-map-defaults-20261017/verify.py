#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check exact M3JDK21 source postimages against Synexia recipe custody."""
from pathlib import Path
import hashlib

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MIRROR = HERE / "recipe-mirror"
EXPECTED = {
    "m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java": (
        "5a8fbb97cd9b5d26c7ac7ab9568ff59808ef03ac27bbaa153739dd5ca03a0f8b",
        "f96f5d027ce046fd69e4220ad48d2848b915ab572ca9e11526bcd5522207e10f",
        "M3FilteredViews.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3ReadOnlyMapDefaultsContract.java": (
        "ABSENT",
        "860a69901dc2959de7d8e56f4e3fe3d188845f9a261c07971c0ea545546c2fe4",
        "M3ReadOnlyMapDefaultsContract.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3ReadOnlyMapDefaultsTest.java": (
        "ABSENT",
        "5dc78eeeee91ad6f77b085b7eebbb464db73a893721769345451ae513ec1b090",
        "M3ReadOnlyMapDefaultsTest.java.txt"),
}

def checked(path):
    if path.is_symlink() or not path.is_file():
        raise SystemExit(f"missing/symbolic source: {path}")
    return path.read_bytes()

def sha(data):
    return hashlib.sha256(data).hexdigest()

manifest = checked(MIRROR / "manifest.tsv").decode("utf-8").splitlines()
parsed = [tuple(row.split("\t")) for row in manifest if row]
if len(parsed) != len(EXPECTED) or any(len(row) != 4 for row in parsed):
    raise SystemExit("unexpected recipe manifest size or schema")
if parsed != sorted(parsed):
    raise SystemExit("unordered recipe manifest")
seen = set()
for path, before, after, name in parsed:
    if path in seen or (path not in EXPECTED) or (before, after, name) != EXPECTED[path]:
        raise SystemExit(f"unexpected target row: {path}")
    seen.add(path)
    template = checked(MIRROR / "templates" / name)
    target = checked(ROOT / path)
    if sha(template) != after or template != target:
        raise SystemExit(f"generated target drift: {path}")
if set(EXPECTED) != seen:
    raise SystemExit("missing canonical Java output")
if sha(checked(MIRROR / "pre" / "M3FilteredViews.java.txt")) != EXPECTED[next(iter(EXPECTED))][0]:
    raise SystemExit("original owner preimage drift")
print("PASS exact M3 filtered Map source mirror: 3 product Java files")
