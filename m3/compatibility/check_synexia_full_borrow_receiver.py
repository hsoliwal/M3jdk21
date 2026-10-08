#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed unless the current Synexia full-borrow receiver remains source-pinned only."""

from __future__ import annotations

import csv
import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "m3" / "synexia-import" / "current-full-borrow"
PIN = BASE / "pin.tsv"
ESTATE = BASE / "synexia-estate.tsv"
STATUS = BASE / "receiver-status.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

PIN_EXPECTED = {
    "schema": "M3JDK21_SYNEXIA_FULL_BORROW_PIN_V1",
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9860",
    "source_revision": "296323958b1019edd59b60b9c05cb148d024cfe5",
    "source_estate_path": ".m3/m3jdk21-full-borrow-estate.tsv",
    "source_estate_git_blob": "1e795e0d781159ff4f96dc3e650deb0cc36c5533",
    "source_canonical_invariant": "docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md",
    "target_repository": "hsoliwal/M3jdk21",
    "target_baseline": "c9b07049c57ecdf43175f5885e11998918416a00",
    "target_estate_copy": "m3/synexia-import/current-full-borrow/synexia-estate.tsv",
    "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
    "first_party_license": "Apache-2.0",
    "abstract_idea_policy": "ABSTRACT_IDEA_NOT_RELABELED_AS_COPYRIGHTED_SOURCE",
    "openjdk_policy": "PRESERVE_EXISTING_OPENJDK_LICENSE_NOTICE",
    "third_party_policy": "SEPARATE_ARTIFACT_LICENSE_REVIEW",
    "initial_target_state": "SOURCE_PIN_ONLY",
    "automatic_application": "false",
    "family_completion": "false",
    "repository_completion": "false",
}

ESTATE_HEADER = [
    "schema",
    "snapshot_revision",
    "family",
    "source_path",
    "source_tree_or_blob",
    "canonical_owner",
    "target_lane",
    "target_surface",
    "license_class",
    "copyright_class",
    "status",
]

STATUS_HEADER = [
    "family",
    "source_tree_or_blob",
    "target_state",
    "target_surface",
    "proof_receipt",
]

REQUIRED_FAMILIES = {
    "TEXT_INDEXSTRING",
    "AST",
    "GRAMMAR",
    "INDEX_PRECOMPUTE",
    "INDEX_PRECOMPUTE_ADAPTERS",
    "INDEXSTRING_COMPILER",
    "INDEXSTRING_JINI",
    "INDEXSTRING_TORNADOVM",
    "MINDEX_RUNTIME",
    "MINDEX_FOUNDATION",
    "DATA_STRUCTURE",
    "COLLECTION",
    "PRECOMPUTE_API",
    "PRECOMPUTE_JINI",
    "COMPILER",
    "ALGORITHM",
    "DATABASE",
    "NATIVE_JNI",
    "SEARCH_REGEX",
    "LOADER",
    "REPOSITORY_INVENTORY",
    "M3INDEX_ALIAS_SURFACE",
    "MINDEX_INDEXSTRING_BRIDGE",
    "MAT_COLLECTION_HISTORY",
    "MAT_INDEX_STRING_HISTORY",
    "NATIVE_INTEROP",
    "FAST_SEARCH",
    "OPENREWRITE_RECIPES",
}

MIXED_COPYRIGHT = "MIXED_FIRST_PARTY_AND_DONOR_REVIEW_REQUIRED"



def git(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        ["git", "-C", str(ROOT), *args],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    if check and result.returncode != 0:
        raise ValueError(f"git {' '.join(args)} failed: {result.stdout.strip()}")
    return result



def git_blob(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def load_pin() -> dict[str, str]:
    with PIN.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.reader(handle, delimiter="\t"))
    if not rows or rows[0] != ["field", "value"]:
        raise ValueError("invalid full-borrow pin header")
    values: dict[str, str] = {}
    for physical, cells in enumerate(rows[1:], start=2):
        if len(cells) != 2 or not cells[0] or not cells[1]:
            raise ValueError(f"invalid pin row {physical}")
        if cells[0] in values:
            raise ValueError(f"duplicate pin field: {cells[0]}")
        values[cells[0]] = cells[1]
    if values != PIN_EXPECTED:
        changed = {k: v for k, v in PIN_EXPECTED.items() if values.get(k) != v}
        extra = {k: v for k, v in values.items() if k not in PIN_EXPECTED}
        raise ValueError(f"full-borrow pin drift changed={changed!r} extra={extra!r}")
    for field in ("source_revision", "source_estate_git_blob", "target_baseline"):
        if not HEX40.fullmatch(values[field]):
            raise ValueError(f"invalid Git identity: {field}")
    if git("merge-base", "--is-ancestor", values["target_baseline"], "HEAD", check=False).returncode != 0:
        raise ValueError("recorded M3JDK target baseline is not an ancestor of tested HEAD")
    return values


def load_estate(pin: dict[str, str]) -> dict[str, dict[str, str]]:
    with ESTATE.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != ESTATE_HEADER:
            raise ValueError("invalid mirrored estate header")
        rows = list(reader)

    if git_blob(ESTATE) != pin["source_estate_git_blob"]:
        raise ValueError("mirrored Synexia estate bytes do not match pinned source Git blob")

    by_family: dict[str, dict[str, str]] = {}
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in ESTATE_HEADER):
            raise ValueError(f"blank estate field at row {physical}")
        if row["schema"] != "SYNEXIA_M3JDK21_FULL_BORROW_V1":
            raise ValueError(f"invalid estate schema at row {physical}")
        if row["snapshot_revision"] != pin["source_revision"]:
            raise ValueError(f"estate source revision drift at row {physical}")
        if not HEX40.fullmatch(row["source_tree_or_blob"]):
            raise ValueError(f"invalid source identity at row {physical}")
        if row["family"] in by_family:
            raise ValueError(f"duplicate estate family: {row['family']}")
        if row["license_class"] != "Apache-2.0":
            raise ValueError(f"non-Apache automatic estate row: {row['family']}")
        if row["status"] != "INVENTORY_PINNED":
            raise ValueError(f"source inventory claims target acceptance: {row['family']}")
        by_family[row["family"]] = row

    if set(by_family) != REQUIRED_FAMILIES:
        raise ValueError(
            f"full-borrow family set drift missing={sorted(REQUIRED_FAMILIES-set(by_family))} "
            f"extra={sorted(set(by_family)-REQUIRED_FAMILIES)}"
        )
    return by_family


def load_status(estate: dict[str, dict[str, str]]) -> None:
    with STATUS.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != STATUS_HEADER:
            raise ValueError("invalid full-borrow receiver-status header")
        rows = list(reader)

    seen: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in STATUS_HEADER):
            raise ValueError(f"blank receiver-status field at row {physical}")
        family = row["family"]
        if family in seen or family not in estate:
            raise ValueError(f"duplicate/unknown receiver family: {family}")
        seen.add(family)
        source = estate[family]
        if row["source_tree_or_blob"] != source["source_tree_or_blob"]:
            raise ValueError(f"receiver source identity drift: {family}")
        if row["target_surface"] != source["target_surface"]:
            raise ValueError(f"receiver target-surface drift: {family}")
        if row["target_state"] != "SOURCE_PIN_ONLY":
            raise ValueError(
                f"initial full-borrow receiver cannot self-promote {family}: {row['target_state']}"
            )
        if row["proof_receipt"] != "NONE":
            raise ValueError(f"source-only receiver cannot invent proof receipt: {family}")
        if source["copyright_class"] == MIXED_COPYRIGHT and row["target_state"] != "SOURCE_PIN_ONLY":
            raise ValueError(f"mixed-license row requires separate artifact review: {family}")

    if seen != set(estate):
        raise ValueError(f"receiver-status missing families: {sorted(set(estate)-seen)}")


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_synexia_full_borrow_receiver.py", file=sys.stderr)
        return 2
    pin = load_pin()
    estate = load_estate(pin)
    load_status(estate)
    mixed = sum(
        row["copyright_class"] == MIXED_COPYRIGHT for row in estate.values()
    )
    print(
        "SYNEXIA_FULL_BORROW_RECEIVER_PASS "
        f"families={len(estate)} source={pin['source_revision']} "
        f"state=SOURCE_PIN_ONLY mixed_review_rows={mixed} completion=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
