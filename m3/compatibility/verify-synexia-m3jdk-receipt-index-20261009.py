# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checker for the observational Synexia-to-M3JDK receipt index.

This validates checked-in custody metadata only. It never promotes code, downloads
payloads, or substitutes for M3JDK hosted/runtime gates.
"""
from __future__ import annotations

import csv
from pathlib import Path
import sys

EXPECTED = {
    "601": ("c2d87c450272dba9ac9b19d61eed30066b1e34d1", "m3/runtime-integration/synexia-ngram-reference-pending-receipt.tsv", "c166015dec7aed0d46a68a1498ccf10cbb879553", "REFERENCE_ONLY_NO_RECEIVER", "NO_PAYLOAD_NO_RECEIVER"),
    "603": ("6e1006399c165f4b52de4b13ca302e838e0c0cd2", "m3/runtime-integration/synexia-frequency-count-pending-receipt.tsv", "547bb234a9446f99e0533deae75e19eedff00d8a", "BLOCKED_UNTIL_SOURCE_PR_ACCEPTED", "NO_PAYLOAD_UNTIL_SOURCE_ADMISSION"),
    "605": ("33af2f2de5273e9f829906b4a89d57fc7c1cfee9", "m3/runtime-integration/synexia-proper-noun-donor-pending-receipt.tsv", "d985991532683daf6565e0793ce086178ede823a", "TARGET_CONTRACT_OPEN_NO_PAYLOAD", "CONTRACT_ONLY_NO_DONOR_PAYLOAD"),
    "607": ("07ed616fc101e1f1b60a0410c4babbe515bdfa81", "m3/lexicon/synexia-m3lexicons-receiver-matrix.tsv", "d42fe084439cbde48b187408ba913443ad42f5f8", "MATRIX_ONLY_OPEN_RECEIVERS", "RECEIPT_ONLY_PER_CAPABILITY"),
    "611": ("a47d1a6f93f9f052660e327b910e22c978e30e5e", "m3/compatibility/synexia-string-current-master-receipt-20261009.tsv", "b70f6a0ba7a6cf9b1b21f67c837e1d976356bb23", "CURRENT_MASTER_OBSERVED", "NO_RUNTIME_PROMOTION"),
    "612": ("f16b011c09f05a7bf7f0ff9f1f8b3cecd03b9913", "m3/docs/synexia-legacy-name-mapping-audit-20261009.tsv", "6bd2c054f64ac4fea3332627314771135b670876", "AUDIT_ONLY_UNPINNED_ROWS", "NO_FABRICATED_TARGETS"),
    "613": ("caaffc69be55a1b41d8754a0b3500fa09c9f2fb6", "m3/compatibility/synexia-number-receiver-receipt-20261009.tsv", "6342516aa4b5041b1567ed29297189ef4a7a8617", "CANDIDATE_RECEIVER_OBSERVED_NO_PROMOTION", "NO_PROMOTION_UNTIL_RECEIVER_GREEN"),
    "614": ("acb61fc3dbd7b4dff236879d93a2cadc89a6b274", "m3/compatibility/synexia-fast-search-donor-receipt-20261009.tsv", "e4630adb09d3b4335ccae61e2de307198496f482", "CANDIDATE_DONOR_REVIEW_RECEIVED_NO_IMPLEMENTATION", "NO_IMPLEMENTATION_RECEIPT_ONLY"),
    "615": ("e26db260088fc85a6e1016091bbddf7556c9d7a4", "m3/compatibility/synexia-z-search-receipt-20261009.tsv", "77fcc34cdcbf3c4e324f7f6da1f522ac701b4159", "CANDIDATE_BOUNDED_PROOF_RECEIVED_NO_RUNTIME_TARGET", "NO_RUNTIME_TARGET"),
    "616": ("ef011714c3326be820c00c1d7a45935dbde735e5", "m3/compatibility/synexia-string-facts-receipt-20261009.tsv", "d03c14e4870832d836cb0447878fa88f742cb990", "SOURCE_MAPPING_RECEIVED_RUNTIME_PENDING", "RUNTIME_PROOF_REQUIRED"),
}
REQUIRED = (
    "receipt_pr", "receipt_head", "receipt_path", "receipt_blob_sha1", "scope",
    "source_pr", "source_head", "target_repo", "target_ref", "receipt_state",
    "runtime_state", "proof_marker", "promotion_decision",
)

def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name("synexia-m3jdk-receipt-index-20261009.tsv")
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = [row for row in csv.reader((line for line in handle if not line.startswith("#")), delimiter="\t") if row]
    if not rows or tuple(rows[0]) != REQUIRED:
        raise SystemExit("M3JDK_SYNEXIA_RECEIPT_INDEX_FAIL header")
    records = [dict(zip(REQUIRED, row)) for row in rows[1:]]
    if len(records) != len(EXPECTED) or {r["receipt_pr"] for r in records} != set(EXPECTED):
        raise SystemExit("M3JDK_SYNEXIA_RECEIPT_INDEX_FAIL row-set")
    for r in records:
        expected_head, expected_path, expected_blob, expected_state, expected_decision = EXPECTED[r["receipt_pr"]]
        actual = (r["receipt_head"], r["receipt_path"], r["receipt_blob_sha1"], r["receipt_state"], r["promotion_decision"])
        if actual != (expected_head, expected_path, expected_blob, expected_state, expected_decision):
            raise SystemExit(f"M3JDK_SYNEXIA_RECEIPT_INDEX_FAIL receipt={r['receipt_pr']}")
        if r["target_repo"] != "hsoliwal/M3jdk21" or r["target_ref"] != "refs/heads/master":
            raise SystemExit(f"M3JDK_SYNEXIA_RECEIPT_INDEX_FAIL target={r['receipt_pr']}")
        if r["runtime_state"] != "NOT_CLAIMED" or "PROMOTED" in r["promotion_decision"] or "GREEN" in r["runtime_state"]:
            raise SystemExit(f"M3JDK_SYNEXIA_RECEIPT_INDEX_FAIL authority={r['receipt_pr']}")
    print("M3JDK_SYNEXIA_RECEIPT_INDEX_PASS rows=10 promotion_authority=NONE runtime_admission=NONE")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
