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

ROOT = Path(__file__).resolve().parents[3]
MAP = Path(__file__).with_name("string-source-target-map.tsv")
ESTATE = Path(__file__).with_name("synexia-estate.tsv")
NAME_MAP = ROOT / "m3/docs/name-mapping.json"
WORK = Path(__file__).with_name("string-phase-work-orders.tsv")
HEX40 = re.compile(r"^[0-9a-f]{40}$")
SUCCESSORS = Path(__file__).with_name("string-target-successors.tsv")
SUCCESSOR_HISTORY = Path(__file__).with_name("string-target-successor-history.tsv")
SUCCESSOR_HISTORY_HEADER = [
    "schema",
    "target_path",
    "predecessor_git_blob",
    "successor_git_blob",
    "successor_commit",
    "change_summary",
    "state",
]
SUCCESSOR_HEADER = [
    "schema",
    "target_path",
    "historical_git_blob",
    "successor_git_blob",
    "synexia_prs",
    "m3jdk_prs",
    "semantic_evidence",
    "state",
]
EXPECTED_SUCCESSOR_PATHS = {
    "src/java.base/share/classes/java/lang/M3String.java",
    "src/java.base/share/classes/java/lang/M3StringFacts.java",
    "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java",
}
SUCCESSOR_MARKERS = {
    "src/java.base/share/classes/java/lang/M3String.java": (
        "private final M3StringOwner owner;",
        "private final long value;",
        "static M3String joinDesignated(",
        "if (left == null) return null;",
        "if (right == null) return null;",
        "return M3StringPool.concat(left, right);",
        "static boolean isLiteralRegex(String regex)",
    ),
    "src/java.base/share/classes/java/lang/M3StringFacts.java": (
        "boolean mayContain(String needle)",
        "boolean prefixMayMatch(String prefix)",
        "boolean suffixMayMatch(String suffix)",
        "if ((bitSignal64 & units) != units) return false;",
    ),
    "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java": (
        "final byte[] firstOffsets;",
        "final long[] masks;",
        "char candidate = source.charAt(blockStart + firstOffsets[mid]);",
        "MAX_SOURCE_UNITS * (Byte.BYTES + Long.BYTES)",
    ),
}

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


def approved_successors(mapping_rows: list[dict[str, str]]) -> dict[str, dict[str, str]]:
    """Admit only exact qualified product postimages without rewriting the donor map."""
    baseline: dict[str, str] = {}
    for row in mapping_rows:
        path = row["target_path"]
        pinned = row["target_git_blob"]
        if path in baseline and baseline[path] != pinned:
            raise ValueError("contradictory historical target pin: " + path)
        baseline[path] = pinned

    received: dict[str, dict[str, str]] = {}
    for index, row in enumerate(load_tsv(SUCCESSORS, SUCCESSOR_HEADER), start=2):
        path = row["target_path"]
        if any(not row[field] for field in SUCCESSOR_HEADER):
            raise ValueError(f"blank successor receipt field at row {index}")
        if row["schema"] != "M3JDK21_STRING_TARGET_SUCCESSOR_V1":
            raise ValueError(f"invalid successor receipt schema at row {index}")
        if path in received:
            raise ValueError("duplicate successor owner: " + path)
        if path not in EXPECTED_SUCCESSOR_PATHS or path not in baseline:
            raise ValueError("unqualified successor owner: " + path)
        if row["historical_git_blob"] != baseline[path]:
            raise ValueError("historical target pin altered: " + path)
        if not HEX40.fullmatch(row["successor_git_blob"]):
            raise ValueError("unsealed successor Git blob: " + path)
        if row["successor_git_blob"] == baseline[path]:
            raise ValueError("successor did not advance original target: " + path)
        for key in ("synexia_prs", "m3jdk_prs"):
            if not re.fullmatch(r"[0-9]+(?:,[0-9]+)*", row[key]):
                raise ValueError("unqualified successor PR lineage: " + path)
        if row["state"] != "HISTORY_PRESERVING_SUCCESSOR":
            raise ValueError("successor promotion policy drift: " + path)
        received[path] = row

    if set(received) != EXPECTED_SUCCESSOR_PATHS:
        raise ValueError(
            "successor inventory drift: missing="
            + repr(sorted(EXPECTED_SUCCESSOR_PATHS - set(received)))
            + " unexpected="
            + repr(sorted(set(received) - EXPECTED_SUCCESSOR_PATHS))
        )
    validate_successor_history(received)
    return received


def validate_successor_history(receipts: dict[str, dict[str, str]]) -> None:
    """Validate the append-only chain of reviewed postimages before accepting its tip."""
    rows = load_tsv(SUCCESSOR_HISTORY, SUCCESSOR_HISTORY_HEADER)
    chains: dict[str, list[dict[str, str]]] = {}
    seen: set[tuple[str, str]] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in SUCCESSOR_HISTORY_HEADER):
            raise ValueError(f"blank successor history field at row {physical}")
        if row["schema"] != "M3JDK21_STRING_SUCCESSOR_HISTORY_V1":
            raise ValueError(f"invalid successor history schema at row {physical}")
        path = row["target_path"]
        if path not in {
            "src/java.base/share/classes/java/lang/M3String.java",
            "src/java.base/share/classes/java/lang/M3StringFacts.java",
        }:
            raise ValueError(f"unqualified successor history owner: {path}")
        for field in ("predecessor_git_blob", "successor_git_blob", "successor_commit"):
            if not HEX40.fullmatch(row[field]):
                raise ValueError(f"unsealed successor history {field} at row {physical}")
        if row["predecessor_git_blob"] == row["successor_git_blob"]:
            raise ValueError(f"successor history did not advance at row {physical}")
        if row["state"] != "HISTORY_PRESERVING_SUCCESSOR":
            raise ValueError(f"successor history policy drift at row {physical}")
        identity = (path, row["successor_git_blob"])
        if identity in seen:
            raise ValueError(f"duplicate successor history postimage: {path}")
        seen.add(identity)
        chains.setdefault(path, []).append(row)
    expected_paths = {
        "src/java.base/share/classes/java/lang/M3String.java",
        "src/java.base/share/classes/java/lang/M3StringFacts.java",
    }
    if set(chains) != expected_paths:
        raise ValueError("successor history inventory drift")
    for path, chain in chains.items():
        for previous, current in zip(chain, chain[1:]):
            if current["predecessor_git_blob"] != previous["successor_git_blob"]:
                raise ValueError(f"broken successor history chain: {path}")
        if chain[-1]["successor_git_blob"] != receipts[path]["successor_git_blob"]:
            raise ValueError(f"successor history tip/receipt mismatch: {path}")

def validate_successor_source(path: str, source: str) -> None:
    for fragment in SUCCESSOR_MARKERS[path]:
        if fragment not in source:
            raise ValueError(f"history-preserving successor responsibility lost: {path} {fragment}")
    if path.endswith("M3StringPositionPrecompute.java"):
        block = source.split("private static final class ExactBlock {", 1)
        if len(block) != 2:
            raise ValueError("position-mask ExactBlock missing")
        exact = block[1].split("private static final class Entry {", 1)[0]
        if re.search(r"(?m)^\s*(?:final\s+)?char\s*\[\s*\]\s+\w+\s*;", exact):
            raise ValueError("position-mask successor reintroduced retained char[] spelling")


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
    successors = approved_successors(rows)
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
        successor = successors.get(row["target_path"])
        pinned = successor["successor_git_blob"] if successor else row["target_git_blob"]
        if actual != pinned:
            raise ValueError(
                f"target owner drift: {row['target_path']} "
                f"expected={pinned} actual={actual}"
            )
        if successor is not None:
            validate_successor_source(row["target_path"], target.read_text(encoding="utf-8"))

    if relations != EXPECTED_RELATIONS:
        raise ValueError(
            f"STRING relation set drift missing={sorted(EXPECTED_RELATIONS-relations)} "
            f"extra={sorted(relations-EXPECTED_RELATIONS)}"
        )

    validate_name_mapping()
    validate_work_orders()
    print(
        "M3JDK21_STRING_PHASE_PIN_PASS "
        f"rows={len(rows)} source_revision={revision} successors={len(successors)} "
        "promotion=false public_api=java.lang.String"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
