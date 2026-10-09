#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checker for the Synexia bounded Z-search receipt."""

from __future__ import annotations

import csv
from pathlib import Path

RECEIPT = Path(__file__).with_name("synexia-z-search-receipt-20261009.tsv")
EXPECTED_PROOF = (
    "M3JDK_SYNEXIA_Z_SEARCH_RECEIPT_PASS "
    "rows=3 state=CANDIDATE_BOUNDED_PROOF_RECEIVED_NO_RUNTIME_TARGET"
)
EXPECTED_SOURCE = (
    "hsoliwal/com.synexia",
    "10043",
    "36ef02cad630276e86a161cdf1af12259762f716",
    "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a",
    "8800",
    "m3-z-search-workspace",
    "synexia-m3-recipe/recipes/m3-z-search-workspace/reconcile-20261009",
    "348dd38844caba42ded1f5f442460386e35323db",
    "3bbcf282be7ffc619323e84a592ca5a67dac532b",
    "96f8d4d2a5741ccf3db61afce5cc2067e5f8f55e",
    "a58f7d8db80e9a855d3f57f97a2203f62cd63c64",
    "ae7320fb95ec4cba7e0b6abba4e5882698335e0e",
    "f6cbc7a745c5d131423408fd1b681120adf8ab15fb156101bf5a7a4aac9591d7",
    "de495c8a5ab4fb346818bf1ef3da4f602e857de3",
    "5f5db167faf521092f842b83866c48c3523dd755",
    "BOUNDED_MAVEN_VERIFY_PASSED|mindex=27144|native=22366|java=6024|jni=6025|ranker=3|fixed_point=true",
    "TheAlgorithms/Java",
    "743ff5eb0944d8a3ed25d73fc992f5a69a36134d",
    "src/main/java/com/thealgorithms/strings/ZAlgorithm.java",
    "f6bcf04e7773b3bfca7a30836672dc7322e38944",
    "MIT-reference-only",
)
EXPECTED_RECEIVER = (
    "hsoliwal/M3jdk21",
    "5f68880c6f4a669e2fe82d6a914c071aef4f80f5",
    "codex/synexia-z-search-receipt-20261009",
    "m3/compatibility/synexia-z-search-receipt-20261009.tsv",
    "RECEIPT_SELF_NOT_ATTESTED",
    "CANDIDATE_BOUNDED_PROOF_RECEIVED_NO_RUNTIME_TARGET",
    "none",
    "NOT_PROMOTED_SCOPE_GAPS_REMAIN",
)
EXPECTED_SLICES = {"JAVA_MINDEX", "JNI_NATIVE_ORACLE", "REPLAY_AND_MAVEN_RECEIPT"}
EXPECTED_HEADER = [
    "slice", "evidence", "source_repo", "source_pr", "source_head",
    "source_base_commit", "source_recipe_pr", "source_recipe_family",
    "source_proof_package", "readme_blob_sha1", "verification_blob_sha1",
    "provenance_blob_sha1", "receipt_files_blob_sha1", "patch_file_blob_sha1",
    "patch_sha256", "maven_log_blob_sha1", "toolchain_blob_sha1",
    "source_proof_marker", "donor_repo", "donor_revision", "donor_path",
    "donor_blob", "donor_license", "receiver_repo", "receiver_base_commit",
    "receiver_branch", "target_path", "target_blob_sha1", "receipt_state",
    "runtime_dependency", "promotion_state",
]


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_SYNEXIA_Z_SEARCH_RECEIPT_FAIL {message}")


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
    rows = list(csv.reader(data_lines, delimiter="\t"))
    if not rows or rows[0] != EXPECTED_HEADER:
        fail("header drift")
    data = rows[1:]
    if len(data) != len(EXPECTED_SLICES):
        fail(f"expected {len(EXPECTED_SLICES)} rows, got {len(data)}")
    seen: set[str] = set()
    for row in data:
        if len(row) != len(EXPECTED_HEADER):
            fail("column-count drift")
        source_slice, evidence = row[:2]
        if tuple(row[2:23]) != EXPECTED_SOURCE:
            fail(f"source custody drift for {source_slice}")
        if tuple(row[23:31]) != EXPECTED_RECEIVER:
            fail(f"receiver boundary drift for {source_slice}")
        if source_slice in seen or source_slice not in EXPECTED_SLICES:
            fail(f"slice coverage drift for {source_slice}")
        if not evidence:
            fail(f"missing evidence for {source_slice}")
        seen.add(source_slice)
    if seen != EXPECTED_SLICES:
        fail("slice set drift")
    print("# proof=" + EXPECTED_PROOF)
    print(EXPECTED_PROOF)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
