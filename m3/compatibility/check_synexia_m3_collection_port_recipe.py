#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed if the pinned Synexia M3JDK collection-port recipe identity drifts."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

PIN = Path(__file__).resolve().parent / "synexia-m3-collection-port-recipe-pin.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

EXPECTED = {
    "schema": "M3JDK21_SYNEXIA_COLLECTION_PORT_RECIPE_PIN_V1",
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9778",
    "source_commit": "a2b8440330c7265a9c94f7ba78e361976d011bab",
    "recipe_name": "com.synexia.rewrite.M3JdkCollectionPortWave1",
    "recipe_yaml_path":
        "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3-jdk-collection-port-wave1.yml",
    "recipe_yaml_blob": "173a6cff09f451e7aa32984f469b72330a47a516",
    "java_manifest_path":
        "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/"
        "m3-jdk-collection-port-wave1/manifest.tsv",
    "java_manifest_blob": "aa2c619dd8711d6ae0f445afabd6809b588e9599",
    "text_manifest_path":
        "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/"
        "m3-jdk-collection-port-wave1/manifest.tsv",
    "text_manifest_blob": "c7eb709bc02364dd75ea9800f34455b8a9f2894a",
    "recipe_test_path":
        "synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/"
        "M3JdkCollectionPortWave1RecipeTest.java",
    "recipe_test_blob": "95bffe90b60945d4ccd668414d277a157d023f21",
    "recipe_workflow_path": ".github/workflows/m3-jdk-collection-port-wave1.yml",
    "recipe_workflow_blob": "b5570914eaf7c2e7030469cdd66180fad8ca46fd",
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
        print("usage: check_synexia_m3_collection_port_recipe.py", file=sys.stderr)
        return 2
    actual = read_pin()
    if actual != EXPECTED:
        changed = {
            key: expected
            for key, expected in EXPECTED.items()
            if actual.get(key) != expected
        }
        extra = {key: value for key, value in actual.items() if key not in EXPECTED}
        raise ValueError(f"Synexia collection-port recipe pin drift changed={changed!r} extra={extra!r}")
    for field in GIT_IDENTITIES:
        if not HEX40.fullmatch(actual[field]):
            raise ValueError(f"invalid Git identity: {field}")
    print(
        "SYNEXIA_M3_COLLECTION_PORT_RECIPE_PIN_PASS "
        f"source={actual['source_commit']} recipe={actual['recipe_name']} "
        "java_base_promotion=false public_jdk_api_change=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
