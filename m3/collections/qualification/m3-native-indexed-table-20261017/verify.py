#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Refuse any drift between the canonical Synexia Maven recipe and actual M3 source."""
from pathlib import Path
import hashlib

HERE = Path(__file__).resolve().parent
REPOSITORY = HERE.parents[3]
RECIPE = HERE / "recipe-mirror"
EXPECTED = {
    "m3/collections/src/main/java/com/m3/collections/M3IndexedTable.java":
        ("M3IndexedTable.java.tpl", "9b87cefcc65e295afe516a1d007e3b5acbc87a8035e1f55223d1b2558cf7858c"),
    "m3/collections/src/test/java/com/m3/collections/M3IndexedTableSelfTest.java":
        ("M3IndexedTableSelfTest.java.tpl", "b6a5ea9b9105b15856af1833026cbe665bc060faccefe7a63973a306de7980ca"),
    "m3/collections/src/test/java/com/m3/collections/M3IndexedTableTest.java":
        ("M3IndexedTableTest.java.tpl", "136b8859525e7a10e10e9fd5184fb6e02f056eb8b85f41a04867079aa1c4ee11"),
}
def exact(path):
    if path.is_symlink() or not path.is_file():
        raise SystemExit(f"Missing or symbolic source path: {path}")
    return path.read_bytes()

receipt = exact(RECIPE / "SOURCE_RECEIPT.tsv").decode("utf-8").splitlines()
if len(receipt) != len(EXPECTED) + 1:
    raise SystemExit("Incomplete source manifest")
seen = set()
for line in receipt[1:]:
    cells = line.split("\t")
    if len(cells) != 5 or cells[1] != "ABSENT":
        raise SystemExit("Malformed or destructive source manifest")
    path = cells[0]
    if path in seen or path not in EXPECTED or cells[2] != EXPECTED[path][1]:
        raise SystemExit(f"Unknown or drifted candidate: {path}")
    seen.add(path)
    template, digest = EXPECTED[path]
    original = exact(RECIPE / "src/main/resources/templates" / template)
    product = exact(REPOSITORY / path)
    if hashlib.sha256(original).hexdigest() != digest or product != original:
        raise SystemExit(f"Receiver is not exact recipe output: {path}")
if seen != set(EXPECTED):
    raise SystemExit("Missing expected source candidates")
print("PASS M3 native indexed table: 3 exact source/template SHA-256 matches")
