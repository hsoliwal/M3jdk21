#!/usr/bin/env python3
"""Source-bound proof for the pending proper-name donor transfer."""
from __future__ import annotations
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RECEIPT = ROOT / "synexia-proper-noun-donor-pending-receipt.tsv"
RECIPE = ROOT / "synexia-proper-noun-donor-pending-recipe-20261009.yaml"

def main() -> int:
    lines = RECEIPT.read_text(encoding="utf-8").splitlines()
    assert len(lines) == 3
    header = lines[0].split("\t")
    rows = [dict(zip(header, line.split("\t"))) for line in lines[1:]]
    assert [row["donor"] for row in rows] == [
        "STEPBible/STEPBible-Data",
        "chakki-works/Japanese-Company-Lexicon",
    ]
    assert all(row["admission"] == "REFERENCE_ONLY" for row in rows)
    assert all(row["target_status"] == "TARGET_CONTRACT_OPEN_NO_PAYLOAD" for row in rows)
    assert all(row["source_pr"] == "10054" for row in rows)
    assert all(row["source_head"] == "92ef3da1b6dff7a125d8d6e9ab6533133fec5ac7" for row in rows)
    recipe = RECIPE.read_text(encoding="utf-8")
    for marker in (
        "target-contract-open-no-payload",
        "donor-data-and-code-must-not-be-copied",
        "domain-donor-must-not-become-general-token-identity",
        "TARGET_CONTRACT_OPEN_NO_PAYLOAD",
    ):
        assert marker in recipe
    print("M3_PROPER_NOUN_DONOR_TARGET_CONTRACT_PASS checks=28/28")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
