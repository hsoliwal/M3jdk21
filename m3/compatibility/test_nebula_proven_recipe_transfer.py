#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RECEIPT = ROOT / "nebula-proven-recipe-transfer.tsv"

EXPECTED_STAGES = [
    "NEBULA_TRANSFER_GATE",
    "NEBULA_99_COVERAGE_ORCHESTRATION",
    "NEBULA_FINAL_LAB",
    "NEBULA_REPROOF",
    "NEBULA_CANONICAL_HOME",
    "NEBULA_FILE_RECIPE_HANDOFF",
    "SYNEXIA_CANONICAL_OWNER",
    "SYNEXIA_NEBULA_PROVEN_RECIPE",
    "M3JDK21_CONSUMER",
]

EXPECTED_REPOS = {
    "NEBULA_TRANSFER_GATE": "hsoliwal/nebula",
    "NEBULA_99_COVERAGE_ORCHESTRATION": "hsoliwal/nebula",
    "NEBULA_FINAL_LAB": "hsoliwal/nebula",
    "NEBULA_REPROOF": "hsoliwal/nebula",
    "NEBULA_CANONICAL_HOME": "hsoliwal/nebula",
    "NEBULA_FILE_RECIPE_HANDOFF": "hsoliwal/nebula",
    "SYNEXIA_CANONICAL_OWNER": "hsoliwal/com.synexia",
    "SYNEXIA_NEBULA_PROVEN_RECIPE": "hsoliwal/com.synexia",
    "M3JDK21_CONSUMER": "hsoliwal/M3jdk21",
}

def load() -> list[dict[str, str]]:
    with RECEIPT.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))

def test_transfer_receipt_is_complete_and_ordered() -> None:
    rows = load()
    assert [row["stage"] for row in rows] == EXPECTED_STAGES
    assert len({row["stage"] for row in rows}) == len(rows)
    assert all(len(row["merge_commit"]) == 40 for row in rows)
    assert all(row["repository"] == EXPECTED_REPOS[row["stage"]] for row in rows)

def test_authority_never_moves_with_recipe_custody() -> None:
    rows = load()
    assert all(row["mutation_authority"] == "false" for row in rows)
    assert all(row["promotion_authority"] == "false" for row in rows)
    assert rows[-1]["role"] == "RECEIVER"

def test_hosted_nebula_reproof_is_not_misreported_as_green() -> None:
    rows = {row["stage"]: row for row in load()}
    assert rows["NEBULA_REPROOF"]["status"] == "HOSTED_ACTIONS_STARTUP_BLOCKED_NO_JOBS"
    assert "zero jobs" in rows["NEBULA_REPROOF"]["notes"]

def test_canonical_ownership_flows_nebula_to_synexia_to_m3jdk21() -> None:
    rows = {row["stage"]: row for row in load()}
    assert rows["NEBULA_FILE_RECIPE_HANDOFF"]["role"] == "TARGET_BINDING"
    assert rows["SYNEXIA_NEBULA_PROVEN_RECIPE"]["role"] == "CANONICAL_RECIPE_HOME"
    assert rows["M3JDK21_CONSUMER"]["role"] == "RECEIVER"

if __name__ == "__main__":
    for test in (
        test_transfer_receipt_is_complete_and_ordered,
        test_authority_never_moves_with_recipe_custody,
        test_hosted_nebula_reproof_is_not_misreported_as_green,
        test_canonical_ownership_flows_nebula_to_synexia_to_m3jdk21,
    ):
        test()
    print("nebula-proven-recipe-transfer: PASS")
