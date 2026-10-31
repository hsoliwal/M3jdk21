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
    "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java": (
        "d67a632f63647f033e71360858f8f1be1f93418880178a96437bb0605998ad5b",
        "3657695ea56c3c01e53d332e37aede2a126efbf718378d217b72d844a4192f86",
        "M3FilteredViews.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3ReadOnlyMapDefaultsContract.java": (
        "ABSENT",
        "9a457bc5418155dcf9ac49e548749e635a17ec94a9ec6761b26b5b3df8a41351",
        "M3ReadOnlyMapDefaultsContract.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3ReadOnlyMapDefaultsTest.java": (
        "ABSENT",
        "7b1779ac790a72d80b555fb7d03d1c79c06e88f08589c6f7e024f8ba46e63783",
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
