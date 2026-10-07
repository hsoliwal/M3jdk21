#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed if the pinned Synexia M3 family naming receiver drifts."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
PIN = HERE / "synexia-m3-family-naming-pin.tsv"
DISPOSITION = HERE / "m3-family-naming-target-disposition.tsv"
PORTING = ROOT / "m3/docs/M3JDK_SYNEXIA_PORTING_INVARIANT.md"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

EXPECTED = {
    "schema": "M3JDK21_SYNEXIA_FAMILY_NAMING_PIN_V1",
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9773",
    "source_commit": "0928e61bff989c7df16761301491bac4e7ba41bc",
    "source_policy_md_path": "docs/M3-SCALE/invariants/M3-FAMILY-NAMING-COLLECTIONS-1.md",
    "source_policy_md_blob": "6ce4ad0811717080abdb128613900f5dfc719db4",
    "source_policy_json_path": "docs/M3-SCALE/invariants/M3-FAMILY-NAMING-COLLECTIONS-1.json",
    "source_policy_json_blob": "ed1a706ccda978309f3cfd0afc40c64b0775a818",
    "source_seed_map_path": "docs/M3-SCALE/invariants/M3-FAMILY-NAMING-COLLECTIONS-1.tsv",
    "source_seed_map_blob": "8c0506ed0c7eb490c3b2b79484a21035afc805e2",
    "source_invariant_table_path": ".m3/m3-recipe-first-invariants.tsv",
    "source_invariant_table_blob": "5b78f91e1b5aa72bf464f562be947530f0758849",
    "source_agents_path": "AGENTS.md",
    "source_agents_blob": "0c40bed9f49a46418507841744aab01f1ea66a44",
    "source_recipe_path": "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3-family-naming-collections.yml",
    "source_recipe_blob": "441aa655a14739b1a544a5b1e77cdfc6cb1145c0",
    "source_recipe_test_path": "synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3FamilyNamingCollectionsRecipeTest.java",
    "source_recipe_test_blob": "65c802a0f4e42bc697fc131fbf60d8f7628d3e20",
    "recipe_name": "com.synexia.rewrite.M3FamilyNamingCollections",
    "reusable_recipe_owner": "hsoliwal/com.synexia",
    "target_repository": "hsoliwal/M3jdk21",
    "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
    "first_party_license": "Apache-2.0",
    "abstract_idea_policy": "PROVENANCE_NOT_COPYRIGHT_OWNERSHIP",
    "third_party_policy": "PRESERVE_ORIGINAL_COPYRIGHT_LICENSE_NOTICE",
    "openjdk_policy": "KEEP_EXISTING_OPENJDK_LICENSE_AND_NOTICE",
    "blind_mindex_to_m3_rename": "false",
    "global_fastest_collection_claim": "false",
    "automatic_materialization": "false",
    "target_runtime_admission": "false",
    "public_jdk_api_change": "false",
}

REQUIRED_FAMILIES = {
    "STRING",
    "ARRAYS",
    "GENERAL_COLLECTIONS",
    "SPECIALIZED_DATA_STRUCTURES",
    "AST_COMPILER",
    "PRECOMPUTE",
    "DAG_GRAPH_RELATION",
    "NATIVE_ACCELERATION",
    "REUSABLE_RECIPES",
}

ALLOWED_STATES = {"EXISTING_TARGET_OWNER", "RECEIVER_MAP_ONLY"}


def read_two_column(path: Path) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        rows = list(csv.reader(stream, delimiter="\t"))
    if not rows or rows[0] != ["field", "value"]:
        raise ValueError("invalid pin header")
    result: dict[str, str] = {}
    for physical, row in enumerate(rows[1:], start=2):
        if len(row) != 2 or not row[0] or not row[1]:
            raise ValueError(f"invalid pin row {physical}")
        if row[0] in result:
            raise ValueError(f"duplicate pin field {row[0]}")
        result[row[0]] = row[1]
    return result


def validate_pin() -> dict[str, str]:
    values = read_two_column(PIN)
    if values != EXPECTED:
        changed = {key: expected for key, expected in EXPECTED.items()
                   if values.get(key) != expected}
        extra = {key: value for key, value in values.items() if key not in EXPECTED}
        raise ValueError(f"Synexia family naming pin drift changed={changed!r} extra={extra!r}")
    for field in (
        "source_commit",
        "source_policy_md_blob",
        "source_policy_json_blob",
        "source_seed_map_blob",
        "source_invariant_table_blob",
        "source_agents_blob",
        "source_recipe_blob",
        "source_recipe_test_blob",
    ):
        if not HEX40.fullmatch(values[field]):
            raise ValueError(f"invalid Git identity in {field}")
    for field in (
        "blind_mindex_to_m3_rename",
        "global_fastest_collection_claim",
        "automatic_materialization",
        "target_runtime_admission",
        "public_jdk_api_change",
    ):
        if values[field] != "false":
            raise ValueError(f"receiver boundary weakened: {field}")
    return values


def validate_dispositions() -> int:
    with DISPOSITION.open("r", encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    if not rows or set(rows[0]) != {
        "family", "sourceRule", "targetNameRule", "targetPackageRule", "targetState", "requiredGate"
    }:
        raise ValueError("invalid target-disposition header")
    names = [row["family"] for row in rows]
    if len(names) != len(set(names)):
        raise ValueError("duplicate target-disposition family")
    if set(names) != REQUIRED_FAMILIES:
        raise ValueError(f"target-disposition family drift: {sorted(set(names) ^ REQUIRED_FAMILIES)}")
    for row in rows:
        if row["targetState"] not in ALLOWED_STATES:
            raise ValueError(f"unqualified target state: {row['family']}={row['targetState']}")
        if not row["requiredGate"].strip():
            raise ValueError(f"missing gate: {row['family']}")
        if "com.synexia" in row["targetPackageRule"]:
            raise ValueError(f"Synexia runtime package leaked into target: {row['family']}")
    return len(rows)


def validate_existing_target_policy() -> None:
    text = PORTING.read_text(encoding="utf-8")
    required = (
        "MIndexString -> M3String",
        "Never mechanically rename",
        "Collections are migrated one concrete semantic owner at a time.",
        "There is no universal",
    )
    missing = [item for item in required if item not in text]
    if missing:
        raise ValueError(f"existing M3JDK porting policy lost required boundaries: {missing!r}")


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_synexia_m3_family_naming.py", file=sys.stderr)
        return 2
    pin = validate_pin()
    rows = validate_dispositions()
    validate_existing_target_policy()
    print(
        "SYNEXIA_M3_FAMILY_NAMING_RECEIVER_PASS "
        f"source={pin['source_commit']} recipe={pin['recipe_name']} "
        f"families={rows} runtime_admission=false public_api_change=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
