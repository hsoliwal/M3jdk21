#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed Phase-1 STRING closure verifier.

Regular mode permits monotonic partial progress while PROMOTION remains BLOCKED.
--require-closed requires every executable gate and PROMOTION itself to be PASS.
"""

from __future__ import annotations

import argparse
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
GATES = Path(__file__).with_name("string-phase-gates.tsv")

EXECUTABLE_GATES = (
    "UTF16_DIFFERENTIAL",
    "OWNER_COORDINATE_LIFETIME",
    "SEARCH_DIFFERENTIAL",
    "REGEX_ORACLE",
    "JNI_SHADOW_LIFETIME",
    "JDK_BUILD",
    "JTREG_NORMAL",
    "JTREG_XINT",
    "JTREG_C1_C2",
    "GC_CDS_JFR_JVMCI",
    "COLD_WARM_RESOURCES",
    "FIXED_POINT_REPLAY",
)

PINNED_GATES = (
    "SOURCE_ESTATE",
    "SOURCE_STRING_ATOMS",
    "TARGET_OWNER_CUSTODY",
    "CANONICAL_NAME_MAPPING",
    "LICENSE_PROVENANCE",
    "COPYRIGHT_NOTICE",
    "ABSTRACT_IDEA_BOUNDARY",
)

RECEIPTS = (
    "m3/compatibility/synexia-string-focused-recipe-receipt-20261008.tsv",
    "m3/compatibility/synexia-regex-oracle-harness-receipt.json",
    "m3/compatibility/synexia-regex-required-literal-receipt.json",
    "m3/compatibility/synexia-string-utf16-differential-receipt.json",
    "m3/compatibility/synexia-string-owner-range-lifetime-receipt.json",
    "m3/compatibility/synexia-string-jni-shadow-lifetime-receipt.json",
    "m3/compatibility/synexia-string-representation-consumers-receipt.json",
    "m3/compatibility/synexia-string-phase1-resources-receipt.json",
)


def load_gates() -> dict[str, str]:
    with GATES.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != ["gate", "state", "evidence_or_requirement"]:
            raise ValueError("STRING gate header drift")
        rows = list(reader)
    states: dict[str, str] = {}
    for row in rows:
        gate = row["gate"]
        if gate in states:
            raise ValueError(f"duplicate STRING gate: {gate}")
        states[gate] = row["state"]
    expected = set(PINNED_GATES) | set(EXECUTABLE_GATES) | {"PROMOTION"}
    if set(states) != expected:
        raise ValueError(
            f"STRING gate set drift missing={sorted(expected-set(states))} "
            f"extra={sorted(set(states)-expected)}"
        )
    return states


def verify_receipts() -> None:
    missing = [path for path in RECEIPTS if not (ROOT / path).is_file()]
    if missing:
        raise ValueError(f"missing STRING proof receipts: {missing}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-closed", action="store_true")
    args = parser.parse_args()

    states = load_gates()
    verify_receipts()

    for gate in PINNED_GATES:
        if states[gate] != "PINNED":
            raise ValueError(f"{gate} lost PINNED custody: {states[gate]}")

    invalid = {
        gate: states[gate]
        for gate in EXECUTABLE_GATES
        if states[gate] not in {"NOT_EXECUTED", "CANDIDATE", "PASS"}
    }
    if invalid:
        raise ValueError(f"invalid executable gate states: {invalid}")

    open_gates = [gate for gate in EXECUTABLE_GATES if states[gate] != "PASS"]
    promotion = states["PROMOTION"]
    if promotion not in {"BLOCKED", "PASS"}:
        raise ValueError(f"invalid PROMOTION state: {promotion}")

    if promotion == "PASS" and open_gates:
        raise ValueError(
            "PROMOTION=PASS with open gates: " + ",".join(open_gates)
        )

    if args.require_closed:
        if open_gates:
            raise ValueError(
                "STRING closure still open: " + ",".join(open_gates)
            )
        if promotion != "PASS":
            raise ValueError("all gates PASS but PROMOTION is not PASS")
        print("M3_STRING_PHASE_CLOSURE_PASS gates=12 promotion=PASS")
        return 0

    if open_gates:
        if promotion != "BLOCKED":
            raise ValueError("open gates require PROMOTION=BLOCKED")
        print(
            "M3_STRING_PHASE_CLOSURE_OPEN "
            f"open={len(open_gates)} promotion=BLOCKED "
            f"gates={','.join(open_gates)}"
        )
    else:
        if promotion == "PASS":
            print("M3_STRING_PHASE_CLOSURE_PASS gates=12 promotion=PASS")
        else:
            print("M3_STRING_PHASE_READY_FOR_PROMOTION gates=12 promotion=BLOCKED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
