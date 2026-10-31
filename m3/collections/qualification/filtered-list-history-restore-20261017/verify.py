#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact noncanonical mirror admission of Synexia #12160 Java source outputs."""
from pathlib import Path
import hashlib

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
MIRROR = HERE / "recipe-mirror"
EXPECTED = {
    "m3/collections/src/main/java/jdk/m3/collections/M3FilteredLists.java": (
        "12bdc2271eee96fb0d24ec188cbcbe37d6c110bf3bb61682715f1af7d5984333",
        "f1e3e21c37cbe084ef6b9446b86293d5039cd9b2bc6c1bf86d63a86793e3b8d5",
        "M3FilteredLists.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3FilteredListsDeepCompositionProof.java": (
        "ABSENT",
        "328c81308abc350e92e98ff0281a8599c72f5032be8816033d34b00fc5a531b5",
        "M3FilteredListsDeepCompositionProof.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3FilteredListsDeepCompositionTest.java": (
        "ABSENT",
        "ba60ff492aa468e97f1e9e9b31719a2918a1eb86574ff723c7ba297ecad5a9f0",
        "M3FilteredListsDeepCompositionTest.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3FilteredListsMaskBridgeProof.java": (
        "ABSENT",
        "762c21a3345d888ac8167fcf058483383f9ee8dba19e9dcdea5c66d35944beac",
        "M3FilteredListsMaskBridgeProof.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3FilteredListsReadonlyDepthProof.java": (
        "ABSENT",
        "d606d65dcfaceaeb5c8ef09b9846be6d99a8a17c3284d25c6ee06f91674af92c",
        "M3FilteredListsReadonlyDepthProof.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3FilteredListsReadonlyDepthTest.java": (
        "ABSENT",
        "b9d1922d94fbaefa8464a8398d8d8a1df0a7c27649ed62edfb776bfecc73e7c3",
        "M3FilteredListsReadonlyDepthTest.java.txt"),
}

def read(path: Path) -> bytes:
    if path.is_symlink() or not path.is_file():
        raise SystemExit(f"missing or symlink source: {path}")
    return path.read_bytes()

manifest = read(MIRROR / "manifest.tsv").decode("utf-8")
rows = [row.split("\t") for row in manifest.splitlines() if row]
if len(rows) != len(EXPECTED):
    raise SystemExit("manifest candidate count drift")
seen = set()
for row in rows:
    if len(row) != 4:
        raise SystemExit("bad candidate manifest width")
    path, before, after, name = row
    if path not in EXPECTED or path in seen or (before, after, name) != EXPECTED[path]:
        raise SystemExit(f"unknown or drifting candidate: {path}")
    seen.add(path)
    exact = read(MIRROR / "templates" / name)
    installed = read(REPO / path)
    if hashlib.sha256(exact).hexdigest() != after or exact != installed:
        raise SystemExit(f"generated Java not byte-exact: {path}")
    if before != "ABSENT":
        source = read(MIRROR / "pre" / name)
        if hashlib.sha256(source).hexdigest() != before or source == exact:
            raise SystemExit(f"historical source preimage drift: {path}")
if seen != set(EXPECTED):
    raise SystemExit("incomplete recipe source coverage")
print(f"PASS M3 filtered List recipe: {len(seen)} exact generated targets; original owner preserved")
