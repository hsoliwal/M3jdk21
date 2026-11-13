#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed if the pinned Synexia M3 family naming receiver drifts."""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
PIN = HERE / "m3-family-naming-pin.tsv"
DISPOSITION = HERE / "m3-family-naming-target-disposition.tsv"
PORTING = ROOT / "m3/docs/M3JDK_M3_PORTING_INVARIANT.md"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

# The pin TSV is the record of a donor handoff: the donor commit, paths and blobs are history as they were at that
# commit, so the record is pinned whole by SHA-256 (LF bytes) and only the receiver policy is spelled out here.
PIN_SHA256 = "35ebfcb690060dc3edd512737f70738a42aadd9994eed1b580f8cbe1866252db"
PIN_FIELDS = (
    "schema",
    "source_repository",
    "source_pr",
    "source_commit",
    "source_policy_md_path",
    "source_policy_md_blob",
    "source_policy_json_path",
    "source_policy_json_blob",
    "source_seed_map_path",
    "source_seed_map_blob",
    "source_invariant_table_path",
    "source_invariant_table_blob",
    "source_agents_path",
    "source_agents_blob",
    "source_recipe_path",
    "source_recipe_blob",
    "source_recipe_test_path",
    "source_recipe_test_blob",
    "recipe_name",
    "reusable_recipe_owner",
    "target_repository",
    "copyright_notice",
    "first_party_license",
    "abstract_idea_policy",
    "third_party_policy",
    "openjdk_policy",
    "blind_mindex_to_m3_rename",
    "global_fastest_collection_claim",
    "automatic_materialization",
    "target_runtime_admission",
    "public_jdk_api_change",
)
EXPECTED = {  # receiver policy and donor identity of the handoff
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9773",
    "source_commit": "a2f1e9e6f84969c04f87cbc5c921d0add8ff71a3",
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
    digest = hashlib.sha256(PIN.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
    changed = {key: expected for key, expected in EXPECTED.items() if values.get(key) != expected}
    if tuple(values) != PIN_FIELDS or changed or digest != PIN_SHA256:
        extra = sorted(set(values) ^ set(PIN_FIELDS))
        raise ValueError(f"Synexia family naming pin drift changed={changed!r} fields={extra!r} sha256={digest}")
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
        print("usage: check_m3_family_naming.py", file=sys.stderr)
        return 2
    pin = validate_pin()
    rows = validate_dispositions()
    validate_existing_target_policy()
    print(
        "M3_FAMILY_NAMING_RECEIVER_PASS "
        f"source={pin['source_commit']} recipe={pin['recipe_name']} "
        f"families={rows} runtime_admission=false public_api_change=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
