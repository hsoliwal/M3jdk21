#!/usr/bin/env python3
"""Verify the Synexia fuzzy-histogram receiver mapping without admitting code."""

from __future__ import annotations

import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "m3/runtime-integration/synexia-fuzzy-histogram-receiver.tsv"
NAME_MAP = ROOT / "m3/docs/name-mapping.json"
HEX40 = re.compile(r"^[0-9a-f]{40}$")
EXPECTED_SOURCE = "com.synexia.indexstring.MIndexPositionMasks"
EXPECTED_TARGET = "java.lang.M3StringPositionPrecompute"
EXPECTED_MANIFEST = "m3/runtime-integration/synexia-fuzzy-histogram-receiver.tsv"


def fail(message: str) -> None:
    raise SystemExit(f"fuzzy receiver mapping rejected: {message}")


def main() -> int:
    with MANIFEST.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        expected = (
            "schema",
            "source_repository",
            "source_commit",
            "source_path",
            "source_blob_sha1",
            "target_repository",
            "target_base",
            "target_owner",
            "target_disposition",
            "recipe_owner",
            "benchmark_repository",
            "benchmark_pr",
            "benchmark_head",
            "benchmark_run",
            "benchmark_job",
            "benchmark_status",
            "target_gate",
            "source_license",
            "target_license",
            "notes",
        )
        if tuple(reader.fieldnames or ()) != expected:
            fail("manifest header drift")
        rows = list(reader)

    if len(rows) != 1:
        fail(f"expected one mapping row, found {len(rows)}")
    row = rows[0]
    if row["schema"] != "M3JDK21_SYNEXIA_RECEIVER_MAP_V1":
        fail("schema drift")
    if row["source_path"] != (
        "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexPositionMasks.java"
    ):
        fail("source path drift")
    if row["source_commit"] != "f938067754ca151c688fe5192d29aab4d415f25f":
        fail("source commit drift")
    if not HEX40.fullmatch(row["source_blob_sha1"]):
        fail("source blob is not a Git SHA-1")
    if row["target_owner"] != EXPECTED_TARGET:
        fail("target owner drift")
    if row["target_disposition"] != "PENDING_BENCHMARK":
        fail("mapping self-promoted")
    if row["benchmark_pr"] != "10112" or row["benchmark_run"] != "38015551577":
        fail("benchmark identity drift")
    if row["benchmark_job"] != "NONE":
        fail("benchmark job drift")
    if row["benchmark_head"] != "e9dfbc3a2fb959de4090edb53503615f1e1c4ec1":
        fail("benchmark head drift")
    if row["benchmark_status"] != "FAILED_NO_JOB":
        fail("benchmark status changed without terminal evidence")
    if row["target_gate"] != "JAVA_JNI_PARITY_AND_NATIVE_BENCHMARK_REQUIRED":
        fail("target gate drift")

    with NAME_MAP.open(encoding="utf-8") as handle:
        mapping = json.load(handle)
    matches = [item for item in mapping.get("mappings", []) if item.get("source") == EXPECTED_SOURCE]
    if len(matches) != 1:
        fail(f"expected one name-mapping entry, found {len(matches)}")
    entry = matches[0]
    if entry.get("target") != EXPECTED_TARGET:
        fail("name-mapping target drift")
    if entry.get("target_disposition") != "PENDING_BENCHMARK":
        fail("name-mapping self-promoted")
    if entry.get("receiver_manifest") != EXPECTED_MANIFEST:
        fail("name-mapping manifest drift")
    if entry.get("benchmark_head") != "e9dfbc3a2fb959de4090edb53503615f1e1c4ec1":
        fail("name-mapping benchmark head drift")
    if entry.get("benchmark_run") != 38015551577 or entry.get("benchmark_job") != "NONE":
        fail("name-mapping benchmark identity drift")

    print(
        "M3_FUZZY_HISTOGRAM_RECEIVER_MAPPING_PASS "
        "source=MIndexPositionMasks "
        "target=M3StringPositionPrecompute "
        "disposition=PENDING_BENCHMARK "
        "promotion=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
