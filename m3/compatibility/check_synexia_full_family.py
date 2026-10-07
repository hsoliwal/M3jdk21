#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed unless the full Synexia MIndex/M3Index family pin stays exact and non-promoting."""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PIN = Path(__file__).with_name("synexia-full-family-pin.tsv")
CATALOGUE = ROOT / "m3/docs/synexia-full-family-catalogue.json"
PRECOMPUTE = ROOT / "m3/docs/synexia-mindex-precompute-inventory.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

EXPECTED = {
    "schema": "M3JDK21_SYNEXIA_FULL_FAMILY_PIN_V1",
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9648",
    "source_commit": "b24262a92b741c78c44ca61afc8dac7ee1376d8a",
    "authority_recovery_pr": "9677",
    "authority_recovery_commit": "73c5e9335b821270d04f2b6912c62fc279a0ba9f",
    "authority_recovery_state": "CURRENT_TREE_EXACT_BLOB_RECOVERY_PR_OPEN",
    "machine_manifest_path": "docs/M3-SCALE/invariants/M3-SYNEXIA-FULL-FAMILY-OWNERSHIP-1.json",
    "machine_manifest_git_blob": "c30a30476c12f7511caa124c4f7e0d72cd4e8e55",
    "receiver_invariant_path": "docs/M3-SCALE/invariants/M3-SYNEXIA-RECEIVER-1.md",
    "receiver_invariant_git_blob": "7ac11f77328c12cea7c157253d99937292235151",
    "catalogue_path": "synexia-openrewrite-recipes/verification/synexia-m3jdk-full-family-20261007/CATALOGUE_INDEX.json",
    "catalogue_git_blob": "8145c667c941a9358e9a5149489e455d950afc34",
    "inventory_source_revision": "7d2133f1412a9e7295296c3f86baae577bb3251c",
    "catalogue_total_paths": "4770",
    "catalogue_checked_roots": "13",
    "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
    "first_party_license": "Apache-2.0",
    "abstract_idea_policy": "ABSTRACT_IDEA_NOT_RELABELED_AS_COPYRIGHTED_SOURCE",
    "third_party_policy": "PRESERVE_ORIGINAL_COPYRIGHT_LICENSE_NOTICE",
    "openjdk_policy": "KEEP_EXISTING_OPENJDK_LICENSE_AND_NOTICE",
    "delivery_state": "SOURCE_PR_OPEN_QUALIFICATION_ONLY",
    "automatic_application": "false",
    "target_relicense_authority": "false",
    "family_completion": "false",
}


def load_pin(path: Path = PIN) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        rows = list(csv.reader(stream, delimiter="\t"))
    if not rows or rows[0] != ["field", "value"]:
        raise ValueError("invalid Synexia full-family pin header")
    values: dict[str, str] = {}
    for physical, cells in enumerate(rows[1:], start=2):
        if len(cells) != 2 or not cells[0] or not cells[1]:
            raise ValueError(f"invalid full-family pin row {physical}")
        if cells[0] in values:
            raise ValueError(f"duplicate full-family pin field {cells[0]}")
        values[cells[0]] = cells[1]
    if values != EXPECTED:
        missing = {k: v for k, v in EXPECTED.items() if values.get(k) != v}
        extra = {k: v for k, v in values.items() if k not in EXPECTED}
        raise ValueError(f"Synexia full-family pin drift missing_or_changed={missing!r} extra={extra!r}")
    for field in (
        "source_commit",
        "authority_recovery_commit",
        "machine_manifest_git_blob",
        "receiver_invariant_git_blob",
        "catalogue_git_blob",
        "inventory_source_revision",
    ):
        if not HEX40.fullmatch(values[field]):
            raise ValueError(f"invalid Git identity in {field}")
    if values["automatic_application"] != "false":
        raise ValueError("full-family automatic application is forbidden")
    if values["target_relicense_authority"] != "false":
        raise ValueError("Synexia handoff never grants target relicensing")
    if values["family_completion"] != "false":
        raise ValueError("unevaluated family inventory cannot be marked complete")
    return values


def validate_catalogue(pin: dict[str, str]) -> dict:
    data = json.loads(CATALOGUE.read_text(encoding="utf-8"))
    if data.get("invariantId") != "M3-SYNEXIA-RECEIVER-1":
        raise ValueError("unexpected full-family catalogue invariant")
    metadata = data.get("metadata") or {}
    source = metadata.get("sourceTreeSelection") or {}
    counts = metadata.get("counts") or {}
    if metadata.get("sourceRevision") != pin["inventory_source_revision"]:
        raise ValueError("full-family catalogue source revision drift")
    if counts.get("total") != int(pin["catalogue_total_paths"]):
        raise ValueError("full-family catalogue total drift")
    roots = source.get("moduleRoots") or []
    if len(roots) != int(pin["catalogue_checked_roots"]):
        raise ValueError("full-family checked-root count drift")
    if source.get("repositoryWideComplete") is not False:
        raise ValueError("scoped catalogue must not claim repository-wide completion")
    if counts.get("byDisposition") != {"NOT_EVALUATED_IN_THIS_LEDGER": int(pin["catalogue_total_paths"])}:
        raise ValueError("full-family catalogue must remain unevaluated at inventory boundary")
    if data.get("acceptance") != "NOT_EVALUATED_IN_THIS_LEDGER":
        raise ValueError("full-family inventory cannot imply target acceptance")
    if data.get("repositoryCompletion") != "NOT_PROVEN":
        raise ValueError("full-family inventory cannot imply repository completion")
    return data


def validate_family_index() -> int:
    lines = [
        line for line in PRECOMPUTE.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    if not lines:
        raise ValueError("empty MIndex/precompute family index")
    header = lines[0].split("\t")
    if header != [
        "sourceWorld",
        "donorFamily",
        "representativeSources",
        "targetDisposition",
        "m3TargetOrRule",
        "notes",
    ]:
        raise ValueError("invalid MIndex/precompute family index header")
    rows = [line.split("\t") for line in lines[1:]]
    required = {
        "Canonical String identity",
        "Regex precompute",
        "AST precompute",
        "DAG precompute",
        "Generic precompute engine",
        "Universal precompute API",
        "Collections/search structures",
        "Native shadow/boundary",
        "Full named-file migration accounting",
    }
    observed = {row[1] for row in rows if len(row) == 6}
    missing = required - observed
    if missing:
        raise ValueError(f"missing full-family receiving categories: {sorted(missing)}")
    full = [row for row in rows if len(row) == 6 and row[1] == "Full named-file migration accounting"]
    if len(full) != 1 or full[0][3] != "NOT_EVALUATED_IN_THIS_LEDGER":
        raise ValueError("full named-file accounting must remain explicitly unevaluated")
    return len(rows)


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_synexia_full_family.py", file=sys.stderr)
        return 2
    pin = load_pin()
    catalogue = validate_catalogue(pin)
    families = validate_family_index()
    print(
        "SYNEXIA_FULL_FAMILY_PIN_PASS "
        f"source={pin['source_commit']} paths={catalogue['metadata']['counts']['total']} "
        f"families={families} license={pin['first_party_license']} completion=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
