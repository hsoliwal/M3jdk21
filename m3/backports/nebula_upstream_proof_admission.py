#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Validate the current upstream-Nebula -> Synexia -> M3JDK21 HOLD receipt."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from pathlib import Path

SCHEMA = "M3JDK21_NEBULA_UPSTREAM_PROOF_ADMISSION_V2"
TARGET = "hsoliwal/M3jdk21"
DEFAULT_RECEIPT = Path(__file__).with_name(
    "NEBULA_UPSTREAM_PROOF_ADMISSION_20261016.tsv"
)

EXPECTED = {
    "schema": SCHEMA,
    "upstream_repository": "EclipseNebula/nebula",
    "upstream_revision": "5d0bcbb9c6e3a118685a5ec91c6f5d8eb7833893",
    "upstream_license": "EPL-2.0",
    "upstream_java": "21",
    "synexia_repository": "hsoliwal/com.synexia",
    "synexia_pr": "11749",
    "synexia_commit": "866052a1b9952f6a087f810f73b07b4e7e06497e",
    "synexia_state": "DRAFT_UNVERIFIED_ACTIONS_INFRASTRUCTURE_FAILURE",
    "synexia_workflow": "M3 Nebula Recipe DAG Convergence",
    "synexia_workflow_state": "FAILED_BEFORE_STEPS_LOGS_UNAVAILABLE",
    "physical_atom_extraction": "HOLD_TRUSTED_VERIFIER_REQUIRED",
    "backport_packet_execution": "false",
    "mutation_authority": "false",
    "promotion_authority": "false",
    "next_action": "WAIT_FOR_SYNEXIA_PR_11749_GREEN",
}


def read(path: Path) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != 1:
        raise ValueError("upstream proof admission must contain exactly one row")
    return rows[0]


def validate(row: dict[str, str]) -> dict[str, str]:
    if row != EXPECTED:
        mismatches = {
            key: (row.get(key), value)
            for key, value in EXPECTED.items()
            if row.get(key) != value
        }
        extra = sorted(set(row) - set(EXPECTED))
        raise ValueError(
            f"upstream proof admission drift: mismatches={mismatches}, extra={extra}"
        )
    for field in ("upstream_revision", "synexia_commit"):
        if not re.fullmatch(r"[0-9a-f]{40}", row[field]):
            raise ValueError(f"invalid Git commit in {field}")
    if row["physical_atom_extraction"] != "HOLD_TRUSTED_VERIFIER_REQUIRED":
        raise ValueError("physical atom extraction requires trusted verifier proof")
    if any(
        row[field] != "false"
        for field in (
            "backport_packet_execution",
            "mutation_authority",
            "promotion_authority",
        )
    ):
        raise ValueError("unverified upstream proof cannot grant execution or authority")
    return row


def canonical_root(row: dict[str, str]) -> str:
    payload = "\t".join(row[key] for key in EXPECTED)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def summary(row: dict[str, str]) -> dict[str, object]:
    validate(row)
    return {
        "schema": SCHEMA,
        "root": canonical_root(row),
        "target": TARGET,
        "upstreamRepository": row["upstream_repository"],
        "upstreamRevision": row["upstream_revision"],
        "synexiaPr": int(row["synexia_pr"]),
        "synexiaCommit": row["synexia_commit"],
        "synexiaState": row["synexia_state"],
        "workflowState": row["synexia_workflow_state"],
        "physicalAtomExtraction": row["physical_atom_extraction"],
        "backportPacketExecution": False,
        "mutationAuthority": False,
        "promotionAuthority": False,
        "nextAction": row["next_action"],
        "disposition": "HOLD",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--receipt", type=Path, default=DEFAULT_RECEIPT)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args(argv)

    payload = summary(read(args.receipt))
    if args.out is not None:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(
            json.dumps(payload, sort_keys=True, indent=2) + "\n",
            encoding="utf-8",
        )
    print(
        "PASS: upstream Nebula proof admission "
        f"disposition={payload['disposition']} root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
