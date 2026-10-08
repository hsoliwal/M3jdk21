#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate the source-bound Phase-1 Synexia MIndexString -> M3JDK M3String mapping."""

from __future__ import annotations

import csv
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAP = Path(__file__).with_name("string-source-target-map.tsv")
ESTATE = Path(__file__).with_name("synexia-estate.tsv")
NAME_MAP = ROOT / "m3/docs/name-mapping.json"
WORK = Path(__file__).with_name("string-phase-work-orders.tsv")
HEX40 = re.compile(r"^[0-9a-f]{40}$")

EXPECTED_RELATIONS = {
    "VALUE_OWNER",
    "VALUE_POOL",
    "VALUE_OWNER_BASE",
    "VALUE_ATOM",
    "VALUE_TUPLE",
    "RANGE_VIEW",
    "FACTS_METADATA",
    "FACTS_TABLE",
    "PRECOMPUTE_CACHE",
    "PRECOMPUTE_IMAGE",
    "PRECOMPUTE_PROFILE",
    "SEARCH_INDEX",
    "BRIDGE",
    "SHADOW",
}

HEADER = [
    "schema",
    "phase",
    "relation",
    "source_path",
    "source_git_blob",
    "target_path",
    "target_git_blob",
    "relationship",
    "license_class",
    "copyright_class",
    "state",
]


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


def load_tsv(path: Path, header: list[str]) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != header:
            raise ValueError(f"invalid header: {path}")
        rows = list(reader)
    if not rows:
        raise ValueError(f"empty table: {path}")
    return rows


def estate_snapshot() -> tuple[str, str]:
    with ESTATE.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    matches = [row for row in rows if row["family"] == "MINDEX_STRING_RUNTIME"]
    if len(matches) != 1:
        raise ValueError("MINDEX_STRING_RUNTIME estate row missing or duplicated")
    row = matches[0]
    if row["source_path"] != (
        "synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexString.java"
    ):
        raise ValueError("MIndexString estate source-path drift")
    if row["source_tree_or_blob"] != "1840f12a3ea64750d829f6dd2bda7916fc25d9f5":
        raise ValueError("MIndexString estate source identity drift")
    return row["snapshot_revision"], row["license_class"]


def validate_name_mapping() -> None:
    data = json.loads(NAME_MAP.read_text(encoding="utf-8"))
    port = data.get("porting_invariant", {})
    if port.get("naming_analogy") != (
        "MIndexString -> java.lang.M3String while public java.lang.String remains String"
    ):
        raise ValueError("canonical MIndexString/M3String naming analogy drift")
    sequence = port.get("receiving_sequence", {})
    if sequence.get("active_phase") != "STRING":
        raise ValueError("STRING is not the active receiving phase")
    if sequence.get("first_party_license") != "Apache-2.0":
        raise ValueError("STRING first-party license policy drift")
    if sequence.get("copyright_notice") != (
        "Copyright 2026 Hitesh Soliwal and contributors"
    ):
        raise ValueError("STRING copyright notice drift")
    if sequence.get("abstract_ideas") != "PROVENANCE_NOT_COPYRIGHTED_EXPRESSION":
        raise ValueError("abstract-idea copyright boundary drift")

    family = data.get("family_name_mapping", [])
    rows = [
        row
        for row in family
        if row.get("target_family") == "java.lang.M3String family"
    ]
    if len(rows) != 1:
        raise ValueError("canonical M3String family map missing or duplicated")
    row = rows[0]
    if row.get("public_api") != "java.lang.String remains java.lang.String":
        raise ValueError("public String naming contract drift")
    required = {
        "java.lang.M3String",
        "java.lang.M3StringOwner",
        "java.lang.M3StringAtom",
        "java.lang.M3StringPool",
        "java.lang.M3StringTuple",
    }
    if set(row.get("target_owners", [])) != required:
        raise ValueError("canonical M3String target owner set drift")



WORK_HEADER = [
    "schema",
    "lane",
    "synexia_pr",
    "synexia_head",
    "m3jdk_pr",
    "m3jdk_head",
    "depends_on",
    "target_gate",
    "state",
]

EXPECTED_WORK_LANES = {
    "STRING_SEARCH",
    "REGEX_ORACLE_10K_X64",
    "REGEX_REQUIRED_LITERAL_GATE",
}


def validate_work_orders() -> None:
    rows = load_tsv(WORK, WORK_HEADER)
    lanes: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in WORK_HEADER):
            raise ValueError(f"blank STRING work-order field at row {physical}")
        if row["schema"] != "M3JDK21_STRING_WORK_V1":
            raise ValueError(f"invalid STRING work-order schema at row {physical}")
        if row["lane"] in lanes:
            raise ValueError(f"duplicate STRING work lane: {row['lane']}")
        lanes.add(row["lane"])
        if not row["synexia_pr"].isdigit() or not row["m3jdk_pr"].isdigit():
            raise ValueError(f"invalid PR number at row {physical}")
        if not HEX40.fullmatch(row["synexia_head"]):
            raise ValueError(f"invalid Synexia head at row {physical}")
        if not HEX40.fullmatch(row["m3jdk_head"]):
            raise ValueError(f"invalid M3JDK head at row {physical}")
        if row["state"] != "DRAFT_CANDIDATE":
            raise ValueError(
                f"STRING work order claims acceptance without successor receipt at row {physical}"
            )
    if lanes != EXPECTED_WORK_LANES:
        raise ValueError(
            f"STRING work lane drift missing={sorted(EXPECTED_WORK_LANES-lanes)} "
            f"extra={sorted(lanes-EXPECTED_WORK_LANES)}"
        )

def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_string_phase.py", file=sys.stderr)
        return 2

    revision, estate_license = estate_snapshot()
    if revision != "296323958b1019edd59b60b9c05cb148d024cfe5":
        raise ValueError("STRING map is not bound to the pinned full-borrow snapshot")
    if estate_license != "Apache-2.0":
        raise ValueError("MIndexString estate lost Apache-2.0 classification")

    rows = load_tsv(MAP, HEADER)
    relations: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in HEADER):
            raise ValueError(f"blank STRING mapping field at row {physical}")
        if row["schema"] != "M3JDK21_STRING_PHASE_V1":
            raise ValueError(f"invalid STRING mapping schema at row {physical}")
        if row["phase"] != "STRING":
            raise ValueError(f"non-STRING row at {physical}")
        if row["relation"] in relations:
            raise ValueError(f"duplicate STRING relation: {row['relation']}")
        relations.add(row["relation"])
        if not row["source_path"].startswith(
            "synexia-mindex/runtime/src/main/java/com/synexia/mindex/"
        ):
            raise ValueError(f"source escaped pinned MIndex runtime at row {physical}")
        if not HEX40.fullmatch(row["source_git_blob"]):
            raise ValueError(f"invalid source Git blob at row {physical}")
        if not row["target_path"].startswith("src/java.base/"):
            raise ValueError(f"target escaped java.base at row {physical}")
        if not HEX40.fullmatch(row["target_git_blob"]):
            raise ValueError(f"invalid target Git blob at row {physical}")
        if row["license_class"] != "Apache-2.0":
            raise ValueError(f"source fast lane lost Apache-2.0 at row {physical}")
        if row["copyright_class"] != "FIRST_PARTY_COPYRIGHTABLE_EXPRESSION":
            raise ValueError(f"copyright classification drift at row {physical}")
        if row["state"] != "PINNED_MAPPING":
            raise ValueError(f"STRING mapping claims promotion at row {physical}")

        target = ROOT / row["target_path"]
        if not target.is_file():
            raise ValueError(f"target owner missing: {row['target_path']}")
        actual = git_blob(target)
        if actual != row["target_git_blob"]:
            raise ValueError(
                f"target owner drift: {row['target_path']} "
                f"expected={row['target_git_blob']} actual={actual}"
            )

    if relations != EXPECTED_RELATIONS:
        raise ValueError(
            f"STRING relation set drift missing={sorted(EXPECTED_RELATIONS-relations)} "
            f"extra={sorted(relations-EXPECTED_RELATIONS)}"
        )

    validate_name_mapping()
    validate_work_orders()
    print(
        "M3JDK21_STRING_PHASE_PIN_PASS "
        f"rows={len(rows)} source_revision={revision} "
        "promotion=false public_api=java.lang.String"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
