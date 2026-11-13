#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed if the pinned Synexia M3JDK collection-port recipe identity drifts."""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

PIN = Path(__file__).resolve().parent / "m3-collection-port-recipe-pin.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

# The pin TSV is the record of a donor handoff: the donor commit, paths and blobs are history as they were at that
# commit, so the record is pinned whole by SHA-256 (LF bytes) and only the receiver policy is spelled out here.
PIN_SHA256 = "82defc0045ac0fa98ca233de722c2edbf5947bf84b55635d5a4ef65610c081ab"
PIN_FIELDS = (
    "schema",
    "source_repository",
    "source_pr",
    "source_commit",
    "recipe_name",
    "recipe_yaml_path",
    "recipe_yaml_blob",
    "java_manifest_path",
    "java_manifest_blob",
    "text_manifest_path",
    "text_manifest_blob",
    "recipe_test_path",
    "recipe_test_blob",
    "recipe_workflow_path",
    "recipe_workflow_blob",
    "first_party_license",
    "reusable_recipe_owner",
    "target_repository",
    "target_mode",
    "java_base_promotion",
    "public_jdk_api_change",
)
EXPECTED = {  # receiver policy and donor identity of the handoff
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9778",
    "source_commit": "a2b8440330c7265a9c94f7ba78e361976d011bab",
    "first_party_license": "Apache-2.0",
    "reusable_recipe_owner": "hsoliwal/com.synexia",
    "target_repository": "hsoliwal/M3jdk21",
    "target_mode": "ISOLATED_PORT_ONLY",
    "java_base_promotion": "false",
    "public_jdk_api_change": "false",
}

GIT_IDENTITIES = {
    "source_commit",
    "recipe_yaml_blob",
    "java_manifest_blob",
    "text_manifest_blob",
    "recipe_test_blob",
    "recipe_workflow_blob",
}


def pin_sha256() -> str:
    return hashlib.sha256(PIN.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def read_pin() -> dict[str, str]:
    with PIN.open("r", encoding="utf-8", newline="") as stream:
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


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_m3_collection_port_recipe.py", file=sys.stderr)
        return 2
    actual = read_pin()
    changed = {key: expected for key, expected in EXPECTED.items() if actual.get(key) != expected}
    if tuple(actual) != PIN_FIELDS or changed or pin_sha256() != PIN_SHA256:
        extra = sorted(set(actual) ^ set(PIN_FIELDS))
        raise ValueError(f"Synexia collection-port recipe pin drift changed={changed!r} fields={extra!r} "
                         f"sha256={pin_sha256()}")
    for field in GIT_IDENTITIES:
        if not HEX40.fullmatch(actual[field]):
            raise ValueError(f"invalid Git identity: {field}")
    print(
        "M3_COLLECTION_PORT_RECIPE_PIN_PASS "
        f"source={actual['source_commit']} recipe={actual['recipe_name']} "
        "java_base_promotion=false public_jdk_api_change=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
