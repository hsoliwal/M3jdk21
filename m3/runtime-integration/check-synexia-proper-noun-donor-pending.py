#!/usr/bin/env python3
"""Source-bound proof for the typed proper-name/title target contract."""
from __future__ import annotations
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RECEIPT = ROOT / "synexia-proper-noun-donor-pending-receipt.tsv"
RECIPE = ROOT / "synexia-proper-noun-donor-pending-recipe-20261009.yaml"
TARGET_MAP = ROOT.parent / "lexicon" / "synexia-instance-target-map.tsv"

def read_tsv(path: Path):
    lines = path.read_text(encoding="utf-8").splitlines()
    assert lines
    header = lines[0].split("\t")
    return header, [dict(zip(header, line.split("\t"))) for line in lines[1:]]

def main() -> int:
    header, rows = read_tsv(RECEIPT)
    assert header[0] == "source_repo"
    assert len(rows) == 2
    assert [row["donor"] for row in rows] == [
        "STEPBible/STEPBible-Data",
        "chakki-works/Japanese-Company-Lexicon",
    ]
    assert all(row["admission"] == "REFERENCE_ONLY" for row in rows)
    assert all(row["target_status"] == "TARGET_CONTRACT_OPEN_NO_PAYLOAD" for row in rows)
    assert all(row["source_pr"] == "10054" for row in rows)
    assert all(row["source_head"] == "5fde33aac53a7999219f86524bb77320f039c58d" for row in rows)
    assert all(row["target_map_path"] == "m3/lexicon/synexia-instance-target-map.tsv"
               for row in rows)
    assert all(row["target_map_blob"] == "5ea190d703ebfe37f91ec63a076e820ccae350e4"
               for row in rows)
    assert all(row["mapping_blob"] == "798e1da0a8f7b326fcdfbff508bcf30ba7c52324" for row in rows)

    map_header, map_rows = read_tsv(TARGET_MAP)
    assert map_header[0] == "synexia_capability"
    assert len(map_rows) == 2
    assert {row["synexia_capability"] for row in map_rows} == {"proper-nouns", "titles"}
    assert all(row["m3jdk_receiver"] == "com.m3.text.M3InstanceIndex" for row in map_rows)
    assert "instanceOfX" in map_rows[0]["target_fields"]
    assert "TitleRecord.identity" in map_rows[1]["target_fields"]
    assert all(row["contract_status"] == "TARGET_CONTRACT_OPEN_NO_PAYLOAD"
               for row in map_rows)
    assert all(row["payload_status"] == "REFERENCE_ONLY_PAYLOAD_NOT_ADMITTED"
               for row in map_rows)

    recipe = RECIPE.read_text(encoding="utf-8")
    for marker in (
        "target-contract-open-no-payload",
        "target_map: m3/lexicon/synexia-instance-target-map.tsv",
        "donor-data-and-code-must-not-be-copied",
        "domain-donor-must-not-become-general-token-identity",
        "TARGET_CONTRACT_OPEN_NO_PAYLOAD",
        "receiver: M3InstanceIndex",
    ):
        assert marker in recipe
    print("M3_PROPER_NOUN_TARGET_RECEIPT_PASS checks=38/38")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
