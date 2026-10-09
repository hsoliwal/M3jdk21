# SPDX-License-Identifier: Apache-2.0
"""Fail-closed custody checker for the current Synexia proper-name receiver chain."""
from __future__ import annotations

import csv
from pathlib import Path
import sys

EXPECTED = {
    "schema_version": "m3-proper-name-instance-v2",
    "capability": "proper-nouns-and-book-titles",
    "source_repo": "hsoliwal/com.synexia",
    "source_audit_head": "fcac99acae04558e03105bc3145d59060093a1dd",
    "source_pr": "10090",
    "source_head": "b6d3038a256e2c7783a606284b24a7cbc9de4142",
    "target_repo": "hsoliwal/M3jdk21",
    "owner_pr": "634",
    "owner_head": "ffd2432d513ebd2e88b5303b3c072d4023e9ef1e",
    "facade_pr": "635",
    "facade_head": "05db1657eb580fdaf65097bcec946806a59bd9a8",
    "owner_type": "com.m3.text.M3InstanceIndexPrecompute",
    "facade_type": "com.m3.text.M3Lexicons",
    "target_status": "OPEN_DRAFT",
    "hosted_status": "NOT_CLAIMED",
    "runtime_status": "NOT_RUN",
    "payload_policy": "NO_PAYLOAD_NO_SECOND_INTERNER",
}

def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name(
        "synexia-proper-name-current-chain-receipt-20261010.tsv"
    )
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = [
            row for row in csv.DictReader(
                (line for line in handle if not line.startswith("#")), delimiter="\t"
            )
        ]
    if len(rows) != 1 or rows[0] != EXPECTED:
        raise SystemExit("M3JDK_SYNEXIA_PROPER_NAME_CHAIN_FAIL custody")
    row = rows[0]
    if row["payload_policy"] != "NO_PAYLOAD_NO_SECOND_INTERNER":
        raise SystemExit("M3JDK_SYNEXIA_PROPER_NAME_CHAIN_FAIL payload")
    if row["hosted_status"] != "NOT_CLAIMED" or row["runtime_status"] != "NOT_RUN":
        raise SystemExit("M3JDK_SYNEXIA_PROPER_NAME_CHAIN_FAIL authority")
    print("M3JDK_SYNEXIA_PROPER_NAME_CHAIN_PASS checks=18 promotion_authority=NONE runtime_admission=NONE")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
