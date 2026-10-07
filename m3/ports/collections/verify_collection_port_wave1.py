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

EXPECTED = {
    "m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveKind.java":
        "4bc7c258073072e0310d1a50c3885ed55c50816b",
    "m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java":
        "2b2f7d73fbcdc1d8ea6b65c0520ca526d7306f4d",
    "m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java":
        "7b2bfc81d5fe8fe69b6c5258d66f521d89ae1dec",
    "m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java":
        "e5a3dcf87e19d38a2fcf07148320919c958d483a",
    "m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java":
        "d1507aac2d65b16f84559caa6d3a02c908bbf1ef",
    "m3/ports/collections/src/main/java/com/m3/ds/M3LongFenwickTree.java":
        "b6c820c05dca67fc53a92a4be84f0b0a2c28b411",
}

SOURCE_COMMIT = "8513abf8eb0d911873c9127a138007a42dd4ab60"
HEX40 = re.compile(r"^[0-9a-f]{40}$")


def fail(message: str) -> None:
    raise ValueError(message)


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
    for row in rows:
        path = row["targetPath"]
        if path in result:
            fail(f"duplicate provenance target: {path}")
        if row["sourceRepository"] != "hsoliwal/com.synexia":
            fail(f"unexpected source repository: {path}")
        if row["sourceCommit"] != SOURCE_COMMIT:
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
        expected_package = "package com.m3.ds;" if "/com/m3/ds/" in relative else "package com.m3.util;"
        if expected_package not in text:
            fail(f"target package mismatch: {relative}")


def verify_recipe() -> None:
    data = json.loads(RECIPE.read_text(encoding="utf-8"))
    expected = {
        "schema": "M3JDK_COLLECTION_PORT_WAVE1_V1",
        "canonical_repository": "hsoliwal/com.synexia",
        "canonical_source_commit": SOURCE_COMMIT,
        "canonical_source_pr": 9778,
        "source_selection_recipe": "com.synexia.rewrite.M3CollectionFacadeWave1",
        "target_repository": "hsoliwal/M3jdk21",
        "mode": "ISOLATED_PORT_ONLY",
        "first_party_license": "Apache-2.0",
        "runtime_synexia_dependency": False,
        "java_base_promotion": False,
        "public_jdk_api_change": False,
        "global_fastest_claim": False,
    }
    for key, value in expected.items():
        if data.get(key) != value:
            fail(f"recipe drift: {key}")
    target_recipe = data.get("target_materialization_recipe")
    if target_recipe not in {
        "PENDING_SYNEXIA_M3JdkCollectionPortWave1",
        "com.synexia.rewrite.M3JdkCollectionPortWave1",
    }:
        fail("unrecognized target materialization recipe")


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: verify_collection_port_wave1.py", file=sys.stderr)
        return 2
    rows = provenance_rows()
    verify_main_sources(rows)
    verify_recipe()
    print(
        "M3_COLLECTION_PORT_WAVE1_PASS "
        f"source={SOURCE_COMMIT} owners={len(EXPECTED)} "
        "runtime_synexia_dependency=false java_base_promotion=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
