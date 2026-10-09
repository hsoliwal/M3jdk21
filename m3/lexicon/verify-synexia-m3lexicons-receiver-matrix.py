#!/usr/bin/env python3
"""Fail-closed structural checker for the Synexia -> M3JDK receiver matrix."""

from __future__ import annotations

import csv
import io
from pathlib import Path
import re
import sys

EXPECTED = {
    "dictlang.dictionary",
    "dictlang.frequency",
    "dictlang.thesaurus",
    "dictlang.antonyms",
    "dictlang.huggingface",
    "unicodex.langdex.lexemes",
    "translate.rows",
    "dictlang.si-units",
    "dictlang.acronyms",
    "dictlang.numbers.0-10000",
    "phrase-rewrite",
    "translation-projection",
    "spell-index",
    "token-hash-precompute",
    "prefix-counts",
    "token-frequency",
    "proper-nouns",
    "titles",
    "string-facts",
    "ngram-reference",
    "rapidex-reference",
}
HEX40 = re.compile(r"^[0-9a-f]{40}$")
STATES = {
    "OPEN",
    "OPEN_DRAFT",
    "MERGED",
    "MERGED_BASE_FOLLOW_UP_OPEN",
    "MERGED_GRAMMAR_OPEN_PROJECTION",
    "REFERENCE_ONLY",
    "NO_FIT",
}
HOSTED = {"NOT_CLAIMED", "LOCAL_PROOF_ONLY", "MERGED_BASE_ONLY", "NOT_APPLICABLE"}
PAYLOAD = {
    "SOURCE_OWNED_NO_EXTERNAL_DATA",
    "SOURCE_LICENSE_ATTACHED",
    "BRIDGE_METADATA_ONLY",
    "REFERENCE_ONLY_UNTIL_PINNED",
    "NO_EXTERNAL_PAYLOAD",
    "NO_CORPUS_ADMITTED",
    "NO_PAYLOAD",
}


def fail(message: str) -> None:
    raise SystemExit(f"M3LEXICONS_RECEIVER_MATRIX_FAIL {message}")


def validate(text: str) -> int:
    rows = list(csv.DictReader(io.StringIO(text), delimiter="\t"))
    if not rows:
        fail("empty")
    required = {
        "capability", "synexia_owner", "m3jdk_target", "receiver_prs",
        "receiver_heads", "source_proof", "target_state", "hosted_state",
        "payload_state", "mapping_rule",
    }
    if set(rows[0]) != required:
        fail(f"columns={sorted(rows[0])}")
    names = [row["capability"] for row in rows]
    if len(names) != len(set(names)):
        fail("duplicate-capability")
    if set(names) != EXPECTED:
        fail(f"capabilities={sorted(set(names))}")
    for row in rows:
        if not all(row[key].strip() for key in required):
            fail(f"blank-field:{row['capability']}")
        if row["target_state"] not in STATES:
            fail(f"target-state:{row['capability']}")
        if row["hosted_state"] not in HOSTED:
            fail(f"hosted-state:{row['capability']}")
        if row["payload_state"] not in PAYLOAD:
            fail(f"payload-state:{row['capability']}")
        heads = [value for value in row["receiver_heads"].split(";") if value != "-"]
        prs = [value for value in row["receiver_prs"].split(";") if value != "-"]
        if len(heads) != len(prs):
            fail(f"pr-head-cardinality:{row['capability']}")
        if any(not HEX40.fullmatch(value) for value in heads):
            fail(f"bad-head:{row['capability']}")
        if row["target_state"] == "NO_FIT" and row["receiver_prs"] != "-":
            fail(f"no-fit-receiver:{row['capability']}")
        if row["target_state"] == "REFERENCE_ONLY" and row["payload_state"] != "NO_CORPUS_ADMITTED":
            fail(f"reference-payload:{row['capability']}")
    return len(rows)


def validate_companions(source_manifest: str, field_map: str) -> None:
    manifest_rows = list(csv.DictReader(io.StringIO(source_manifest), delimiter="\t"))
    acronym_rows = [row for row in manifest_rows if row.get("source_id") == "dictlang.acronyms"]
    if len(acronym_rows) != 1:
        fail("acronym-manifest-cardinality")
    acronym_fields = acronym_rows[0].get("precompute_fields", "")
    if acronym_fields != "acronym,domain,expansion":
        fail(f"acronym-precompute-fields={acronym_fields}")
    field_rows = list(csv.DictReader(io.StringIO(field_map), delimiter="\t"))
    expected = {
        ("AcronymPrecompute", "acronym", "acronym"),
        ("AcronymPrecompute", "domain", "domain"),
        ("AcronymPrecompute", "expansion", "expansion"),
    }
    actual = {
        (row.get("donor_type", ""), row.get("donor_field", ""), row.get("canonical_payload_field", ""))
        for row in field_rows
    }
    if not expected.issubset(actual):
        fail("acronym-field-map-incomplete")


if __name__ == "__main__":
    count = validate(sys.stdin.read())
    root = Path(__file__).resolve().parent
    validate_companions(
        (root / "synexia-source-manifest.tsv").read_text(encoding="utf-8"),
        (root / "synexia-precompute-field-map.tsv").read_text(encoding="utf-8"),
    )
    print(f"M3LEXICONS_RECEIVER_MATRIX_PASS rows={count} acronym_fields=3")
