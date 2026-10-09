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
    assert all(row["target_status"] == "NO_TYPED_RECEIVER" for row in rows)
    assert all(row["source_pr"] == "10054" for row in rows)
    assert all(row["source_head"] == "051fecfcd7b2ad1bdfb1666b2faa0e0a52820168" for row in rows)
    recipe = RECIPE.read_text(encoding="utf-8")
    for marker in (
        "pending-reference-only",
        "donor-data-and-code-must-not-be-copied",
        "domain-donor-must-not-become-general-token-identity",
        "NO_TYPED_RECEIVER",
    ):
        assert marker in recipe
    print("M3_PROPER_NOUN_DONOR_PENDING_SOURCE_PASS checks=24/24")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
