#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Validate the current proof-gated Nebula -> Synexia -> M3JDK21 admission receipt."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from pathlib import Path

SCHEMA = "M3JDK21_NEBULA_PROOF_ADMISSION_V1"
TARGET = "hsoliwal/M3jdk21"
DEFAULT_RECEIPT = Path(__file__).with_name("NEBULA_PROOF_GATED_ADMISSION_20261015.tsv")

EXPECTED = {
    "schema": SCHEMA,
    "nebula_repository": "hsoliwal/nebula",
    "nebula_pr": "212",
    "nebula_commit": "9a77803ce174be534ba5f543a9f3b342858d2bbc",
    "nebula_state": "HOLD_UNTIL_PROVEN",
    "synexia_repository": "hsoliwal/com.synexia",
    "synexia_pr": "11632",
    "synexia_commit": "cf21063f2620ca12202122643bf7d4120ed16194",
    "synexia_state": "DRAFT_UNMERGED",
    "canonical_recipe_dag": "com.synexia.rewrite.M3NebulaRecipeDag",
    "canonical_viewport_recipe": "com.synexia.rewrite.M3NebulaViewportDistillationRecipe",
    "target_repository": TARGET,
    "packet_execution": "false",
    "mutation_authority": "false",
    "promotion_authority": "false",
    "next_action": "WAIT_FOR_NEBULA_GREEN_AND_SYNEXIA_MERGE",
}


def read(path: Path) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="	"))
    if len(rows) != 1:
        raise ValueError("proof-gated admission must contain exactly one row")
    return rows[0]


def validate(row: dict[str, str]) -> dict[str, str]:
    if row != EXPECTED:
        missing = {k: (row.get(k), v) for k, v in EXPECTED.items() if row.get(k) != v}
        extra = sorted(set(row) - set(EXPECTED))
        raise ValueError(f"proof-gated admission drift: mismatches={missing}, extra={extra}")
    for field in ("nebula_commit", "synexia_commit"):
        if not re.fullmatch(r"[0-9a-f]{40}", row[field]):
            raise ValueError(f"invalid Git commit in {field}")
    if any(row[field] != "false" for field in (
        "packet_execution", "mutation_authority", "promotion_authority"
    )):
        raise ValueError("HOLD admission cannot grant execution/mutation/promotion authority")
    return row


def canonical_root(row: dict[str, str]) -> str:
    payload = "	".join(row[key] for key in EXPECTED)
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def summary(row: dict[str, str]) -> dict[str, object]:
    validate(row)
    return {
        "schema": SCHEMA,
        "root": canonical_root(row),
        "target": TARGET,
        "nebulaState": row["nebula_state"],
        "synexiaState": row["synexia_state"],
        "packetExecution": False,
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
        args.out.write_text(json.dumps(payload, sort_keys=True, indent=2) + "
", encoding="utf-8")
    print(
        "PASS: Nebula proof-gated admission "
        f"disposition={payload['disposition']} root={payload['root']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
