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
    "m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java": (
        "7a9d818d38beae4df178317f7913050b47d6442e85f1ef5d5873a0df89262101",
        "b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d",
        "M3FilteredLists.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3FilteredListsDeepCompositionProof.java": (
        "ABSENT",
        "7226c2622ffe49bbf4d6299a9c79bf284f849a6d44cfb966f0082038b765a89c",
        "M3FilteredListsDeepCompositionProof.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3FilteredListsDeepCompositionTest.java": (
        "ABSENT",
        "6fd5e3956d5a0b1dc443d48b5f0691cff213c5a5f67f128be6a8080e6a6bd32e",
        "M3FilteredListsDeepCompositionTest.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3FilteredListsMaskBridgeProof.java": (
        "ABSENT",
        "02d4463f758ed57c9d5ca27c8892f5fe5776e034a0f03bae8767fde8d61a6363",
        "M3FilteredListsMaskBridgeProof.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3FilteredListsReadonlyDepthProof.java": (
        "ABSENT",
        "7476bd46c9bbd465d54237e3dd106d71725e8d8ba74a73d2001878b53bb22f5c",
        "M3FilteredListsReadonlyDepthProof.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3FilteredListsReadonlyDepthTest.java": (
        "ABSENT",
        "4c443dfe22e908747b6cc1267c25e42fa9252b4936d47e27a8b55d3d50bf5e8b",
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
