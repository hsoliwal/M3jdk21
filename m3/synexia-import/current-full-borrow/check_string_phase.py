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
    "src/java.base/share/classes/java/lang/M3StringPool.java",
}
# Additive, candidate-only source chronology; historical four-row successor receipt remains immutable.
SUCCESSOR_CHAIN = Path(__file__).with_name("string-target-successor-chain.tsv")
SUCCESSOR_CHAIN_HEADER = [
    "schema", "target_path", "predecessor_git_blob", "successor_git_blob",
    "synexia_pr", "m3jdk_pr", "state",
]
PRECOMPUTE_OBSERVATION = Path(__file__).with_name("m3-string-precompute-observation-v2.tsv")
PRECOMPUTE_OBSERVATION_HEADER = [
    "schema",
    "snapshot_date_utc",
    "source_symbol",
    "source_path",
    "source_git_blob",
    "source_license",
    "source_relation",
    "cache_symbol",
    "cache_path",
    "cache_git_blob",
    "cache_relation",
    "target_symbol",
    "target_path",
    "target_license",
    "checkpoint_m3jdk_pr",
    "checkpoint_m3jdk_merge_commit",
    "checkpoint_target_git_blob",
    "observation_m3jdk_pr",
    "observation_m3jdk_head",
    "observation_m3jdk_merge_commit",
    "observation_target_git_blob",
    "current_m3jdk_pr",
    "current_m3jdk_merge_commit",
    "current_m3jdk_master_commit",
    "current_target_git_blob",
    "synexia_source_recipe_pr",
    "synexia_source_recipe_head",
    "synexia_source_recipe_state_at_capture",
    "synexia_donor_decision_pr",
    "synexia_donor_decision_role",
    "implementation_origin",
    "runtime_acceptance",
    "performance_evidence",
    "state",
]
EXPECTED_PRECOMPUTE_OBSERVATION = {
    "schema": "M3JDK21_STRING_PRECOMPUTE_OBSERVATION_V2",
    "snapshot_date_utc": "2026-10-21",
    "source_symbol": "com.synexia.indexstring.MIndexStringSearchPlan",
    "source_path": "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringSearchPlan.java",
    "source_git_blob": "402e6d8cf2f99d19ffc9c1291afee7baf80665ec",
    "source_license": "Apache-2.0",
    "source_relation": "canonical operation-search facts; source mapping only",
    "cache_symbol": "com.synexia.mindex.MIndexStringPrecomputeCache",
    "cache_path": "synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexStringPrecomputeCache.java",
    "cache_git_blob": "0e777d4fe11e169aab7c495e334316e443142f16",
    "cache_relation": "CONSUMER_SPLIT_PRECOMPUTE; source mapping only",
    "target_symbol": "java.lang.M3StringSearchPrecompute",
    "target_path": "src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java",
    "target_license": "GPL-2.0-with-Classpath-exception",
    "checkpoint_m3jdk_pr": "864",
    "checkpoint_m3jdk_merge_commit": "fc57c84cbbd48c6fe25113a683992d7495aec7e5",
    "checkpoint_target_git_blob": "dfbb91cc169916cd0a95798f4cd2151c3407df4c",
    "observation_m3jdk_pr": "869",
    "observation_m3jdk_head": "e0422f994559a2d9c2dee3b9b2af768a2b69c70a",
    "observation_m3jdk_merge_commit": "d5abed865bd47c4dd5ad78e95704348fe2095ed2",
    "observation_target_git_blob": "ac68eacd6ee4e6048ab424542ccf5c2cf5595908",
    "current_m3jdk_pr": "920",
    "current_m3jdk_merge_commit": "3d9d44eeed2859fe5c3d353723e7b627b379205d",
    "current_m3jdk_master_commit": "2779aba3f98f31c324be43f731be7914e1a05935",
    "current_target_git_blob": "6f89d8cc25c8d8b7d372faaa7a9a550f819868c3",
    "synexia_source_recipe_pr": "11559",
    "synexia_source_recipe_head": "46d4d4864fcbffda3538e594f6be9babc0f88407",
    "synexia_source_recipe_state_at_capture": "OPEN_DRAFT",
    "synexia_donor_decision_pr": "11270",
    "synexia_donor_decision_role": "FIRST_PARTY_REUSE_INVENTORY_ONLY",
    "implementation_origin": "M3JDK_TARGET_LOCAL_CURSOR_AND_M3TQ_REUSE_NO_SYNEXIA_CODE_COPY",
    "runtime_acceptance": "NOT_CLAIMED",
    "performance_evidence": "NOT_MEASURED",
    "state": "SOURCE_OBSERVATION_NOT_RUNTIME_ACCEPTED",
}
PRECOMPUTE_SEARCH_MAPPING_SOURCE = (
    "com.synexia.indexstring.MIndexStringSearchPlan and canonical operation-search facts"
)
PRECOMPUTE_SEARCH_MAPPING_RULE = (
    "weakly key exact M3 owner+coordinate; retain primitive pattern metadata only; "
    "no String/char[]/byte[]/M3String payload; eviction changes performance only"
)

M3_STRING_PATH = "src/java.base/share/classes/java/lang/M3String.java"
# Each exact transition was independently observed at the named GitHub PR head.
EXPECTED_STRING_CHAIN = (
    ("70d1d03c109126fdda36c54da21b484bbe368fd9",
     "4fb87fd70ffe5c8d75d17db9c28b44bc9b574704", "11220", "862"),
    ("4fb87fd70ffe5c8d75d17db9c28b44bc9b574704",
     "42399d411c1516a56a47dcdb10242960157b4677", "11247", "864"),
    ("42399d411c1516a56a47dcdb10242960157b4677",
     "35ed998540e044eed8cb7596e2e2c019ae369bd3", "11448", "898"),
)

SUCCESSOR_MARKERS = {
    "src/java.base/share/classes/java/lang/M3String.java": (
        "private final M3StringOwner owner;",
        "private final long value;",
        "static M3String joinDesignated(",
        "M3String left = canonicalize(first);",
        "if (left == null) return null;",
        "M3String right = canonicalize(second);",
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
    "src/java.base/share/classes/java/lang/M3StringPool.java": (
        "if (left.length() == 0) return right;",
        "if (right.length() == 0) return left;",
        "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {",
        "throw new OutOfMemoryError(\"Required length exceeds implementation limit\");",
        "if (left.owner() == right.owner() && left.end() == right.start()) {",
        "return M3String.range(",
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
    return received


def current_successor_blobs(mapping_rows: list[dict[str, str]]) -> dict[str, str]:
    """Extend approved historical successors using an exact serial proof chain.

    These are *candidate source identity* receipts, not runtime or phase acceptance.
    Wrong source, wrong order, changed PR evidence, missing or extra steps fail closed.
    """
    historical = approved_successors(mapping_rows)
    current = {target: row["successor_git_blob"] for target, row in historical.items()}
    chain = load_tsv(SUCCESSOR_CHAIN, SUCCESSOR_CHAIN_HEADER)
    if len(chain) != len(EXPECTED_STRING_CHAIN):
        raise ValueError("String successor chain length drift")
    for index, (step, expected) in enumerate(zip(chain, EXPECTED_STRING_CHAIN), start=1):
        before, after, synexia_pr, m3jdk_pr = expected
        if any(not step[field] for field in SUCCESSOR_CHAIN_HEADER):
            raise ValueError(f"blank String successor chain field: {index}")
        if (step["schema"] != "M3JDK21_STRING_TARGET_EVOLUTION_V1"
                or step["target_path"] != M3_STRING_PATH
                or step["predecessor_git_blob"] != before
                or step["successor_git_blob"] != after
                or step["synexia_pr"] != synexia_pr
                or step["m3jdk_pr"] != m3jdk_pr
                or step["state"] != "SOURCE_CANDIDATE_NOT_RUNTIME_ACCEPTED"):
            raise ValueError(f"unreviewed String successor chain step: {index}")
        if current[M3_STRING_PATH] != before:
            raise ValueError(f"discontinuous String successor chain: {index}")
        current[M3_STRING_PATH] = after
    return current



def load_precompute_observation() -> dict[str, str]:
    """Read the exact V2 lineage receipt; candidate facts never imply acceptance."""
    rows = load_tsv(PRECOMPUTE_OBSERVATION, PRECOMPUTE_OBSERVATION_HEADER)
    if len(rows) != 1:
        raise ValueError("M3String precompute observation cardinality drift")
    row = rows[0]
    if any(not row[field] for field in PRECOMPUTE_OBSERVATION_HEADER):
        raise ValueError("blank M3String precompute observation field")
    if row != EXPECTED_PRECOMPUTE_OBSERVATION:
        raise ValueError("unreviewed M3String precompute observation drift")
    for field in (
        "source_git_blob", "cache_git_blob", "checkpoint_m3jdk_merge_commit",
        "checkpoint_target_git_blob", "observation_m3jdk_head",
        "observation_m3jdk_merge_commit", "observation_target_git_blob",
        "current_m3jdk_merge_commit", "current_m3jdk_master_commit",
        "current_target_git_blob", "synexia_source_recipe_head",
    ):
        if not HEX40.fullmatch(row[field]):
            raise ValueError("unsealed M3String precompute observation: " + field)
    for field in (
        "checkpoint_m3jdk_pr", "observation_m3jdk_pr", "current_m3jdk_pr",
        "synexia_source_recipe_pr", "synexia_donor_decision_pr",
    ):
        if not row[field].isdigit():
            raise ValueError("invalid M3String precompute PR: " + field)
    target = ROOT / row["target_path"]
    if target.is_file() and git_blob(target) != row["current_target_git_blob"]:
        raise ValueError("live M3JDK precompute target blob drift")
    return row


def validate_precompute_mapping(mapping_rows: list[dict[str, str]],
                                observation: dict[str, str]) -> None:
    matches = [
        row for row in mapping_rows
        if row["relation"] == "PRECOMPUTE_CACHE"
        and row["source_path"] == observation["cache_path"]
    ]
    if len(matches) != 1:
        raise ValueError("MIndexString precompute source mapping missing or duplicated")
    row = matches[0]
    if (row["source_git_blob"] != observation["cache_git_blob"]
            or row["target_path"] != observation["target_path"]
            or row["relationship"] != "CONSUMER_SPLIT_PRECOMPUTE"
            or row["license_class"] != observation["source_license"]
            or row["state"] != "PINNED_MAPPING"):
        raise ValueError("MIndexString precompute mapping contract drift")


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

    if path.endswith("M3StringPool.java"):
        ordered = (
            "if (left.length() == 0) return right;",
            "if (right.length() == 0) return left;",
            "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {",
            "throw new OutOfMemoryError(\"Required length exceeds implementation limit\");",
            "if (left.owner() == right.owner() && left.end() == right.start()) {",
            "return concatBalanced(left, right);",
        )
        positions = [source.find(fragment) for fragment in ordered]
        if any(position < 0 for position in positions) or positions != sorted(positions):
            raise ValueError("M3StringPool concat length successor guard order drift")


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

    search_maps = [
        item for item in data.get("mappings", [])
        if item.get("source") == PRECOMPUTE_SEARCH_MAPPING_SOURCE
        and item.get("target") == "java.lang.M3StringSearchPrecompute"
    ]
    if len(search_maps) != 1:
        raise ValueError("canonical MIndexString/M3StringSearchPrecompute map missing")
    search_map = search_maps[0]
    if (search_map.get("status") != "internal-only bounded operation-precompute adaptation"
            or search_map.get("mapping_rule") != PRECOMPUTE_SEARCH_MAPPING_RULE):
        raise ValueError("M3StringSearchPrecompute internal mapping contract drift")



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
    current_blobs = current_successor_blobs(rows)
    precompute_observation = load_precompute_observation()
    validate_precompute_mapping(rows, precompute_observation)
    if precompute_observation["target_path"] in current_blobs:
        raise ValueError("M3String precompute owner collides with V1 successor chain")
    current_blobs[precompute_observation["target_path"]] = (
        precompute_observation["current_target_git_blob"]
    )
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
        pinned = current_blobs.get(row["target_path"], row["target_git_blob"])
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
