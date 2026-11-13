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

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "m3" / "m3-import" / "intakes" / "full-borrow-20261011"
PIN = BASE / "pin.tsv"
ESTATE = BASE / "m3-estate.tsv"
DAG = BASE / "m3-dag.tsv"
PHASES = BASE / "promotion-phases.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

# The pin TSV is the record of a donor intake: the donor commit, paths and blobs are history as they were at that
# commit, so the record is pinned whole by SHA-256 (LF bytes) and only the receiver policy is spelled out here.
PIN_SHA256 = "8bfda1f10efe07ec8482ca79efbcc0e7f71fc53e8472b750b31b762d4fb672fc"
PIN_FIELDS = (
    "schema",
    "source_repository",
    "source_pr",
    "source_revision",
    "source_handoff_commit",
    "source_estate_path",
    "source_estate_git_blob",
    "source_dag_path",
    "source_dag_git_blob",
    "source_phase_path",
    "source_phase_git_blob",
    "source_family_count",
    "target_repository",
    "target_baseline",
    "target_branch",
    "target_estate_copy",
    "target_dag_copy",
    "target_phase_copy",
    "copyright_notice",
    "first_party_license",
    "openjdk_policy",
    "third_party_policy",
    "canonical_owner",
    "canonical_invariant",
    "receiver_role",
    "generic_ownership_transfer",
    "target_generic_reusable_owner",
    "reusable_change_return_path",
    "target_specific_authority",
    "initial_target_state",
    "automatic_application",
    "promotion_authority",
    "repository_completion",
)
PIN_EXPECTED = {  # receiver policy and donor identity of the intake
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "10336",
    "source_revision": "37e4edabea6af918a4dc9a7eabd45fbdcf101881",
    "source_handoff_commit": "3a87d17df7e5d825aa1b071c5fb05c64b854c798",
    "source_estate_git_blob": "f637e78db34ab86d3e0d3fc717f03e05867d9d53",
    "source_dag_git_blob": "3e6c56b8555231fb36d0ac620ab112aa5970d45a",
    "source_phase_git_blob": "c4e8582f99caba32d718b2c1e86dae0854cdce3d",
    "source_family_count": "32",
    "target_repository": "hsoliwal/M3jdk21",
    "target_baseline": "985aadda29756c0fe988d7b70c8240d4eb554dc7",
    "target_branch": "master",
    "target_estate_copy": "m3/m3-import/intakes/full-borrow-20261011/m3-estate.tsv",
    "target_dag_copy": "m3/m3-import/intakes/full-borrow-20261011/m3-dag.tsv",
    "target_phase_copy": "m3/m3-import/intakes/full-borrow-20261011/promotion-phases.tsv",
    "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
    "first_party_license": "Apache-2.0",
    "openjdk_policy": "PRESERVE_EXISTING_OPENJDK_LICENSE_NOTICE",
    "third_party_policy": "SEPARATE_ARTIFACT_LICENSE_REVIEW",
    "canonical_owner": "hsoliwal/com.synexia",
    "receiver_role": "TARGET_RECEIVER_ONLY",
    "generic_ownership_transfer": "false",
    "target_generic_reusable_owner": "false",
    "target_specific_authority": "OPENJDK_INTEGRATION_JTREG_RUNTIME_AND_FINAL_PROMOTION",
    "initial_target_state": "SOURCE_PIN_ONLY",
    "automatic_application": "false",
    "promotion_authority": "false",
    "repository_completion": "false",
}
# The estate, DAG and phase copies are the donor's bytes (pinned by their donor Git blobs in the pin): their schema
# values are the donor's, so each file must carry one schema value ending with its suffix below.
ESTATE_SCHEMA = "_M3JDK21_FULL_BORROW_V1"
DAG_SCHEMA = "_M3JDK21_FULL_BORROW_DAG_V1"
PHASE_SCHEMA = "_M3JDK21_PROMOTION_PHASE_V1"

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
    digest = hashlib.sha256(PIN.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
    changed = {key: expected for key, expected in PIN_EXPECTED.items() if values.get(key) != expected}
    if tuple(values) != PIN_FIELDS or changed or digest != PIN_SHA256:
        extra = sorted(set(values) ^ set(PIN_FIELDS))
        raise ValueError(f"successor intake pin drift changed={changed!r} fields={extra!r} sha256={digest}")
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
        if row["schema"] != rows[0]["schema"] or not row["schema"].endswith(ESTATE_SCHEMA):
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
        if row["schema"] != rows[0]["schema"] or not row["schema"].endswith(DAG_SCHEMA):
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
        if row["schema"] != rows[0]["schema"] or not row["schema"].endswith(PHASE_SCHEMA):
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
        print("usage: check_m3_full_borrow_intake_20261011.py", file=sys.stderr)
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
        "M3_FULL_BORROW_SUCCESSOR_PASS "
        f"families={len(estate)} source={pin['source_revision']} "
        f"handoff={pin['source_handoff_commit']} state=SOURCE_PIN_ONLY "
        f"mixed_review_rows={mixed} receiver=TARGET_RECEIVER_ONLY "
        "automatic_application=false promotion_authority=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
