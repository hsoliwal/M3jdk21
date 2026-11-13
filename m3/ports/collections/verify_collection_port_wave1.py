#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the isolated M3 collection port and its pinned Synexia provenance."""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
PROVENANCE = ROOT / "PROVENANCE.tsv"
RECIPE = ROOT / "recipe.json"
# Standalone (2026-10-31): the donor repository is lineage, kept once in the standalone ledger and never
# fetched; this verifier compares provenance against that ledger row (make/m3/tooling/standalone/M3_REFS.tsv).
LINEAGE = REPO / "make" / "m3" / "tooling" / "standalone" / "LINEAGE_SOURCES.tsv"
LINEAGE_KEY = "synexia"

EXPECTED = {
    "m3/collections/src/main/java/jdk/m3/collections/M3DenseHash.java":
        "7e0111b3df43a626de36ffb12f1c81903c41b9b8",
    "m3/collections/src/main/java/jdk/m3/collections/M3PrimitiveKind.java":
        "bbfa6200d5e8583cf414e51f48b546fe5496fb7f",
    "m3/collections/src/main/java/jdk/m3/collections/M3PrimitiveArrayList.java":
        "026de38364777dbbd0718f6317743a6b7a7ca13e",
    "m3/collections/src/main/java/jdk/m3/collections/M3PrimitiveArrayDeque.java":
        "7b2bfc81d5fe8fe69b6c5258d66f521d89ae1dec",
    "m3/collections/src/main/java/jdk/m3/collections/M3IntArrayDeque.java":
        "653d76e199ea69f9aec45f2f533b8b50d92ce844",
    "m3/collections/src/main/java/jdk/m3/collections/M3PrimitiveMinMaxQueue.java":
        "d278d98daaf048ffa78b51322016887b9b1ab609",
    "m3/collections/src/main/java/jdk/m3/collections/M3LongFenwickTree.java":
        "b6c820c05dca67fc53a92a4be84f0b0a2c28b411",
}

SOURCE_COMMIT = "8513abf8eb0d911873c9127a138007a42dd4ab60"; REFRESH_COMMITS = frozenset({"fd9a233680b43197eb8c5745a729c342ca56c4e2"})  # CONVERGE_HUB_REFRESH v1, 2026-11-06
HEX40 = re.compile(r"^[0-9a-f]{40}$")


def fail(message: str) -> None:
    raise ValueError(message)


def lineage_repository() -> str:
    """The donor repository of this port, read from the standalone lineage ledger."""
    with LINEAGE.open("r", encoding="utf-8", newline="") as stream:
        rows = {row["key"]: row for row in csv.DictReader(stream, delimiter="\t")}
    if LINEAGE_KEY not in rows or not rows[LINEAGE_KEY].get("repository"):
        fail(f"missing lineage source: {LINEAGE_KEY}")
    return rows[LINEAGE_KEY]["repository"]


def provenance_rows() -> dict[str, dict[str, str]]:
    with PROVENANCE.open("r", encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    required = {
        "targetPath", "sourceRepository", "sourceCommit", "sourcePath",
        "sourceBlob", "license", "materialization"
    }
    if not rows or set(rows[0]) != required:
        fail("invalid provenance header")
    result: dict[str, dict[str, str]] = {}
    repository = lineage_repository()
    for row in rows:
        path = row["targetPath"]
        if path in result:
            fail(f"duplicate provenance target: {path}")
        if row["sourceRepository"] != repository:
            fail(f"unexpected source repository: {path}")
        if row["sourceCommit"] != SOURCE_COMMIT and row["sourceCommit"] not in REFRESH_COMMITS:
            fail(f"source commit drift: {path}")
        if row["license"] != "Apache-2.0":
            fail(f"license drift: {path}")
        if not HEX40.fullmatch(row["sourceBlob"]):
            fail(f"invalid source blob: {path}")
        result[path] = row
    return result


def verify_main_sources(rows: dict[str, dict[str, str]]) -> None:
    for relative, blob in EXPECTED.items():
        row = rows.get(relative)
        if row is None or row["sourceBlob"] != blob:
            fail(f"provenance mismatch: {relative}")
        path = REPO / relative
        text = path.read_text(encoding="utf-8")
        if f"Source-Git-blob: {blob}" not in text:
            fail(f"source blob header mismatch: {relative}")
        if "import com.synexia" in text or "package com.synexia" in text:
            fail(f"Synexia runtime package leaked into port: {relative}")
        expected_package = "package jdk.m3.collections;"  # one home since 2026-11-06 (CONSOLIDATE_PORT_OWNERS v1)
        if expected_package not in text:
            fail(f"target package mismatch: {relative}")


def verify_recipe() -> None:
    data = json.loads(RECIPE.read_text(encoding="utf-8"))
    expected = {
        "schema": "M3JDK_COLLECTION_PORT_WAVE1_V1",
        "canonical_repository": lineage_repository(),
        "canonical_source_commit": SOURCE_COMMIT,
        "canonical_source_pr": 9778,
        "source_selection_recipe": "jdk.internal.m3.rewrite.M3CollectionFacadeWave1",
        "target_repository": "hsoliwal/M3jdk21",
        "mode": "ISOLATED_PORT_ONLY",
        "first_party_license": "Apache-2.0",
        "runtime_m3_dependency": False,
        "java_base_promotion": False,
        "public_jdk_api_change": False,
        "global_fastest_claim": False,
    }
    for key, value in expected.items():
        if data.get(key) != value:
            fail(f"recipe drift: {key}")
    if data.get("target_materialization_recipe") != "jdk.internal.m3.rewrite.M3JdkCollectionPortWave1":
        fail("target materialization recipe drift")


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: verify_collection_port_wave1.py", file=sys.stderr)
        return 2
    rows = provenance_rows()
    verify_main_sources(rows)
    verify_recipe()
    print(
        "M3_COLLECTION_PORT_WAVE1_PASS "
        f"source={SOURCE_COMMIT} refresh={','.join(sorted(REFRESH_COMMITS))} owners={len(EXPECTED)} "
        "runtime_m3_dependency=false java_base_promotion=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
