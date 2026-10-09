#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checker for the Synexia category-first fast-search receipt."""

from __future__ import annotations

import csv
from pathlib import Path

RECEIPT = Path(__file__).with_name("synexia-fast-search-donor-receipt-20261009.tsv")
EXPECTED_PROOF = (
    "M3JDK_SYNEXIA_FAST_SEARCH_DONOR_RECEIPT_PASS "
    "rows=9 state=CANDIDATE_DONOR_REVIEW_RECEIVED_NO_IMPLEMENTATION"
)
EXPECTED_SOURCE = (
    "hsoliwal/com.synexia",
    "10045",
    "5b3ca1a6875f1364e6c4832feca09baea9f87390",
    "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a",
    "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/fast-search-donor-review/catalogue.tsv",
    "b147ca4fa4e6589d086ddc5e98b70be8caa94c22",
    "f6010de645e569cf40f5261d2c6d43e606ce515f7846fd62ba3898a66f74b9b5",
    "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3FastSearchDonorPassLedger.java",
    "c7bb4485cfdf34c41b4bab158a5bc1831a735d44",
    "synexia-openrewrite-recipes/verification/donor-review/M3FastSearchDonorPassProof.java",
    "164b25df2e470ee7e0e13c10c60c3443ab8c079f",
    "synexia-openrewrite-recipes/docs/M3_FAST_SEARCH_DONOR_SERIAL_REVIEW_20261009.md",
    "7fd9551c3376b9c0792212932d1789196f9f439a",
    "DONOR_LEDGER_JAVA21_PASS|rows=9|platforms=GEEKSFORGEEKS,GITHUB,HACKERRANK,LEETCODE|passes=45|root=99a482567e8bcb28ea168dbf71f54d8c2f16f6a8d7f27deabc151ab8055e25c7",
    "Apache-2.0;BSD-3-Clause;NO_CODE_LICENSE",
)
EXPECTED_RECEIVER = (
    "hsoliwal/M3jdk21",
    "5f68880c6f4a669e2fe82d6a914c071aef4f80f5",
    "codex/synexia-fast-search-donor-receipt-20261009",
    "m3/compatibility/synexia-fast-search-donor-receipt-20261009.tsv",
    "RECEIPT_SELF_NOT_ATTESTED",
    "CANDIDATE_DONOR_REVIEW_RECEIVED_NO_IMPLEMENTATION",
    "none",
    "NOT_PROMOTED_NO_TARGET_IMPLEMENTATION",
)
EXPECTED_IDS = {
    "LC-28", "GFG-Z-2026", "HR-STRING-SIMILARITY", "ABSEIL-STRMATCH",
    "HR-CONTACTS", "GFG-AHO-2025", "GOOGLE-RE2J", "GFG-RABIN-2026",
    "GOOGLE-GUAVA-BLOOM",
}
EXPECTED_HEADER = [
    "operation", "platform", "source_id", "source_repo", "source_pr",
    "source_head", "source_base_commit", "source_catalogue_path",
    "source_catalogue_blob_sha1", "source_catalogue_sha256", "source_ledger_path",
    "source_ledger_blob_sha1", "source_proof_path", "source_proof_blob_sha1",
    "source_doc_path", "source_doc_blob_sha1", "source_proof_marker",
    "source_license", "receiver_repo", "receiver_base_commit", "receiver_branch",
    "target_path", "target_blob_sha1", "receipt_state", "runtime_dependency",
    "promotion_state",
]


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_SYNEXIA_FAST_SEARCH_DONOR_RECEIPT_FAIL {message}")


def main() -> int:
    if not RECEIPT.is_file():
        fail("receipt missing")
    comments: list[str] = []
    data_lines: list[str] = []
    for line in RECEIPT.read_text(encoding="utf-8").splitlines():
        if line.startswith("#"):
            comments.append(line)
        elif line.strip():
            data_lines.append(line)
    if "# proof=" + EXPECTED_PROOF not in comments:
        fail("proof marker drift")
    rows = list(csv.reader(data_lines, delimiter="\\t"))
    if not rows or rows[0] != EXPECTED_HEADER:
        fail("header drift")
    data = rows[1:]
    if len(data) != len(EXPECTED_IDS):
        fail(f"expected {len(EXPECTED_IDS)} rows, got {len(data)}")
    seen: set[str] = set()
    for row in data:
        if len(row) != len(EXPECTED_HEADER):
            fail("column-count drift")
        operation, platform, source_id = row[:3]
        source = tuple(row[3:18])
        receiver = tuple(row[18:26])
        if source != EXPECTED_SOURCE:
            fail(f"source custody drift for {source_id}")
        if receiver != EXPECTED_RECEIVER:
            fail(f"receiver boundary drift for {source_id}")
        if source_id in seen or source_id not in EXPECTED_IDS:
            fail(f"source coverage drift for {source_id}")
        if platform not in {"LEETCODE", "HACKERRANK", "GEEKSFORGEEKS", "GITHUB"}:
            fail(f"platform drift for {source_id}")
        if not operation:
            fail(f"empty operation for {source_id}")
        seen.add(source_id)
    if seen != EXPECTED_IDS:
        fail("source set drift")
    print("# proof=" + EXPECTED_PROOF)
    print(EXPECTED_PROOF)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
