#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed unless the 2026-10-11 Synexia successor intake is exact and receiver-only."""

from __future__ import annotations

import csv
import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "m3" / "synexia-import" / "intakes" / "full-borrow-20261011"
PIN = BASE / "pin.tsv"
ESTATE = BASE / "synexia-estate.tsv"
DAG = BASE / "synexia-dag.tsv"
PHASES = BASE / "promotion-phases.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

PIN_EXPECTED = {
    "schema": "M3JDK21_SYNEXIA_FULL_BORROW_INTAKE_V1",
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "10336",
    "source_revision": "37e4edabea6af918a4dc9a7eabd45fbdcf101881",
    "source_handoff_commit": "3a87d17df7e5d825aa1b071c5fb05c64b854c798",
    "source_estate_path": ".m3/m3jdk21-full-borrow-estate.tsv",
    "source_estate_git_blob": "ddcec769cb1833fab5f224a41c7f6ab86295d262",
    "source_dag_path": ".m3/m3jdk21-full-borrow-dag.tsv",
    "source_dag_git_blob": "3e6c56b8555231fb36d0ac620ab112aa5970d45a",
    "source_phase_path": ".m3/m3jdk21-promotion-phases.tsv",
    "source_phase_git_blob": "c4e8582f99caba32d718b2c1e86dae0854cdce3d",
    "source_family_count": "32",
    "target_repository": "hsoliwal/M3jdk21",
    "target_baseline": "985aadda29756c0fe988d7b70c8240d4eb554dc7",
    "target_branch": "master",
    "target_estate_copy": "m3/synexia-import/intakes/full-borrow-20261011/synexia-estate.tsv",
    "target_dag_copy": "m3/synexia-import/intakes/full-borrow-20261011/synexia-dag.tsv",
    "target_phase_copy": "m3/synexia-import/intakes/full-borrow-20261011/promotion-phases.tsv",
    "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
    "first_party_license": "Apache-2.0",
    "openjdk_policy": "PRESERVE_EXISTING_OPENJDK_LICENSE_NOTICE",
    "third_party_policy": "SEPARATE_ARTIFACT_LICENSE_REVIEW",
    "canonical_owner": "hsoliwal/com.synexia",
    "canonical_invariant": "SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1",
    "receiver_role": "TARGET_RECEIVER_ONLY",
    "generic_ownership_transfer": "false",
    "target_generic_reusable_owner": "false",
    "reusable_change_return_path": "TARGET_DISCOVERY_TO_SYNEXIA_MASTER_THEN_RESEALED_TARGET",
    "target_specific_authority": "OPENJDK_INTEGRATION_JTREG_RUNTIME_AND_FINAL_PROMOTION",
    "initial_target_state": "SOURCE_PIN_ONLY",
    "automatic_application": "false",
    "promotion_authority": "false",
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

DAG_HEADER = [
    "schema",
    "family",
    "depends_on",
    "promotion_stage",
    "inventory_parallel",
    "target_gate",
    "state",
]

PHASE_HEADER = [
    "schema",
    "phase",
    "predecessor",
    "source_families",
    "target_authority",
    "state",
]

EXPECTED_PHASE_ORDER = [
    "STRING",
    "ARRAYS",
    "COLLECTIONS",
    "AST_COMPILER",
    "REMAINING_FAMILIES",
]


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
    with PIN.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.reader(stream, delimiter="\t"))
    if not rows or rows[0] != ["field", "value"]:
        raise ValueError("invalid successor intake pin header")
    values: dict[str, str] = {}
    for physical, cells in enumerate(rows[1:], start=2):
        if len(cells) != 2 or not cells[0] or not cells[1]:
            raise ValueError(f"invalid successor intake pin row {physical}")
        if cells[0] in values:
            raise ValueError(f"duplicate successor intake pin field: {cells[0]}")
        values[cells[0]] = cells[1]
    if values != PIN_EXPECTED:
        changed = {key: expected for key, expected in PIN_EXPECTED.items()
                   if values.get(key) != expected}
        extra = {key: value for key, value in values.items()
                 if key not in PIN_EXPECTED}
        raise ValueError(f"successor intake pin drift changed={changed!r} extra={extra!r}")
    for field in (
        "source_revision",
        "source_handoff_commit",
        "source_estate_git_blob",
        "source_dag_git_blob",
        "source_phase_git_blob",
        "target_baseline",
    ):
        if not HEX40.fullmatch(values[field]):
            raise ValueError(f"invalid Git identity: {field}")
    if git("merge-base", "--is-ancestor", values["target_baseline"], "HEAD", check=False).returncode != 0:
        raise ValueError("recorded target baseline is not an ancestor of tested HEAD")
    return values


def load_estate(pin: dict[str, str]) -> dict[str, dict[str, str]]:
    with ESTATE.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != ESTATE_HEADER:
            raise ValueError("invalid successor estate header")
        rows = list(reader)
    if git_blob(ESTATE) != pin["source_estate_git_blob"]:
        raise ValueError("successor estate bytes do not match pinned Synexia Git blob")
    by_family: dict[str, dict[str, str]] = {}
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in ESTATE_HEADER):
            raise ValueError(f"blank successor estate field at row {physical}")
        if row["schema"] != "SYNEXIA_M3JDK21_FULL_BORROW_V1":
            raise ValueError(f"invalid successor estate schema at row {physical}")
        if row["snapshot_revision"] != pin["source_revision"]:
            raise ValueError(f"successor estate revision drift at row {physical}")
        if row["family"] in by_family:
            raise ValueError(f"duplicate successor family: {row['family']}")
        if not HEX40.fullmatch(row["source_tree_or_blob"]):
            raise ValueError(f"invalid successor source identity at row {physical}")
        if not row["canonical_owner"].startswith("com.synexia"):
            raise ValueError(f"non-Synexia canonical owner at row {physical}")
        if row["license_class"] != "Apache-2.0":
            raise ValueError(f"non-Apache automatic row at {row['family']}")
        if row["status"] != "INVENTORY_PINNED":
            raise ValueError(f"source inventory self-promoted at {row['family']}")
        by_family[row["family"]] = row
    if len(by_family) != int(pin["source_family_count"]):
        raise ValueError("successor family count drift")
    return by_family


def load_dag(pin: dict[str, str], estate: dict[str, dict[str, str]]) -> None:
    with DAG.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != DAG_HEADER:
            raise ValueError("invalid successor DAG header")
        rows = list(reader)
    if git_blob(DAG) != pin["source_dag_git_blob"]:
        raise ValueError("successor DAG bytes do not match pinned Synexia Git blob")
    families = set(estate)
    graph: dict[str, set[str]] = {}
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in DAG_HEADER):
            raise ValueError(f"blank successor DAG field at row {physical}")
        family = row["family"]
        if row["schema"] != "SYNEXIA_M3JDK21_FULL_BORROW_DAG_V1":
            raise ValueError(f"invalid successor DAG schema at row {physical}")
        if family in graph or family not in families:
            raise ValueError(f"duplicate/unknown successor DAG family: {family}")
        if row["inventory_parallel"] != "true":
            raise ValueError(f"successor inventory parallelism drift: {family}")
        if row["state"] != "PLANNED":
            raise ValueError(f"successor DAG self-promoted: {family}")
        deps = set() if row["depends_on"] == "-" else set(row["depends_on"].split(";"))
        unknown = deps - families
        if family in deps or unknown:
            raise ValueError(f"invalid successor dependencies for {family}: {sorted(unknown)}")
        graph[family] = deps
    if set(graph) != families:
        raise ValueError("successor DAG family set drift")
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(family: str) -> None:
        if family in visited:
            return
        if family in visiting:
            raise ValueError(f"cycle in successor DAG at {family}")
        visiting.add(family)
        for dependency in sorted(graph[family]):
            visit(dependency)
        visiting.remove(family)
        visited.add(family)

    for family in sorted(graph):
        visit(family)


def load_phases(pin: dict[str, str], estate: dict[str, dict[str, str]]) -> None:
    with PHASES.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != PHASE_HEADER:
            raise ValueError("invalid successor promotion-phase header")
        rows = list(reader)
    if git_blob(PHASES) != pin["source_phase_git_blob"]:
        raise ValueError("successor phase bytes do not match pinned Synexia Git blob")
    if [row["phase"] for row in rows] != EXPECTED_PHASE_ORDER:
        raise ValueError("successor promotion phase order drift")
    families = set(estate)
    referenced: set[str] = set()
    previous = "-"
    for row in rows:
        if row["schema"] != "SYNEXIA_M3JDK21_PROMOTION_PHASE_V1":
            raise ValueError("invalid successor promotion schema")
        if row["predecessor"] != previous:
            raise ValueError(f"successor phase predecessor drift: {row['phase']}")
        if row["state"] != "PLANNED":
            raise ValueError(f"successor phase self-promoted: {row['phase']}")
        phase_families = set(row["source_families"].split(";"))
        unknown = phase_families - families
        if unknown:
            raise ValueError(f"successor phase references unknown families: {sorted(unknown)}")
        referenced.update(phase_families)
        previous = row["phase"]
    if referenced != families:
        raise ValueError(f"successor phase coverage drift missing={sorted(families-referenced)}")


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_synexia_full_borrow_intake_20261011.py", file=sys.stderr)
        return 2
    pin = load_pin()
    estate = load_estate(pin)
    load_dag(pin, estate)
    load_phases(pin, estate)
    mixed = sum(
        row["copyright_class"] == "MIXED_FIRST_PARTY_AND_DONOR_REVIEW_REQUIRED"
        for row in estate.values()
    )
    print(
        "SYNEXIA_FULL_BORROW_SUCCESSOR_PASS "
        f"families={len(estate)} source={pin['source_revision']} "
        f"handoff={pin['source_handoff_commit']} state=SOURCE_PIN_ONLY "
        f"mixed_review_rows={mixed} receiver=TARGET_RECEIVER_ONLY "
        "automatic_application=false promotion_authority=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
