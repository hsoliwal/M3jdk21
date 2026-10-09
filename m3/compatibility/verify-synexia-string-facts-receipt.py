#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checker for the Synexia string-facts owner mapping receipt."""

from __future__ import annotations

import csv
from pathlib import Path

RECEIPT = Path(__file__).with_name("synexia-string-facts-receipt-20261009.tsv")
EXPECTED_PROOF = (
    "M3JDK_SYNEXIA_STRING_FACTS_RECEIPT_PASS "
    "rows=5 state=SOURCE_MAPPING_RECEIVED_RUNTIME_PENDING"
)
EXPECTED_SOURCE = (
    "hsoliwal/com.synexia",
    "10039",
    "2d70798ee2890baf34b8de530bdde783412cdf87",
    "c6d128572825cc11f4bbb05d78bcbebb5b3aa67a",
    "synexia-m3-recipe/recipes/mindex-string-facts-owner-20261009.yaml",
    "419deeba24baca90f57a77807a6878b5a4b969c8",
    "synexia-m3-recipe/verification/mindex-string-facts-owner-20261009/source_discriminator.py",
    "4d03c22b01c7ec45ad4a80671fbe0c2051cc4447",
    "synexia-m3-recipe/verification/mindex-string-facts-owner-20261009/tests/test_source_discriminator.py",
    "22f0d889432da4b0d819eb100b42329b47c1cf06",
    "FOSS_REUSE_DECISION.tsv",
    "019db5b7790756ee23eaef487e4609a5e822c3ce",
    "Apache-2.0;first-party",
    "M3_STRING_FACTS_OWNER_SOURCE_PASS checks=100/100|runtime=NOT_RUN|maven=NOT_RUN|native=NOT_RUN|junit-JTReg=NOT_RUN|benchmark=NOT_MEASURED|hosted-ci=NOT_CLAIMED",
)
EXPECTED_RECEIVER = (
    "hsoliwal/M3jdk21",
    "5f68880c6f4a669e2fe82d6a914c071aef4f80f5",
    "master",
    "m3/compatibility/synexia-string-facts-receipt-20261009.tsv",
    "RECEIPT_SELF_NOT_ATTESTED",
    "SOURCE_MAPPING_RECEIVED_RUNTIME_PENDING",
    "NOT_PROMOTED_RUNTIME_PROOF_REQUIRED",
)
EXPECTED_MAPPINGS = {
    "IndexTextMetrics": (
        "java.lang.M3StringFacts",
        "synexia-indexstring/src/main/java/com/synexia/indexstring/IndexTextMetrics.java",
        "58beddc813b3ed4716233d23f0e1909bda4e21b9",
        "SOURCE_PROVEN_RUNTIME_PENDING",
    ),
    "MIndexTextPrecomputedFacts": (
        "java.lang.M3StringFacts",
        "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexTextPrecomputedFacts.java",
        "701c28acf980c409dcfb541c8dbb0438ad72ab85",
        "SOURCE_PROVEN_RUNTIME_PENDING",
    ),
    "MIndexStringCanonicalFacts": (
        "java.lang.M3StringFacts + java.lang.M3StringOwner + java.lang.M3StringTuple",
        "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringCanonicalFacts.java",
        "1200c927da62570b5881a9c9667b7e3a44d1d11a",
        "SOURCE_PROVEN_RUNTIME_PENDING",
    ),
    "MIndexUtf16RangeFacts": (
        "java.lang.M3StringOwner.rangeFacts",
        "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexUtf16RangeFacts.java",
        "6a10a38f6639d8244a378d484c198c10d349705b",
        "SOURCE_PROVEN_RUNTIME_PENDING",
    ),
    "MIndexStringPrecomputation": (
        "caller-owned M3 mapped/precompute services",
        "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringPrecomputation.java",
        "b762518f382b22b0622d4fac57985f47e58adf11",
        "SOURCE_ONLY_NO_JAVA_LANG_STRING_PAYLOAD",
    ),
}
EXPECTED_HEADER = [
    "donor_owner", "target_owner", "source_path", "source_blob_sha1",
    "mapping_status", "source_repo", "source_pr", "source_head",
    "source_base_commit", "source_recipe_path", "source_recipe_blob_sha1",
    "source_discriminator_path", "source_discriminator_blob_sha1",
    "source_test_path", "source_test_blob_sha1", "foss_decision_path",
    "foss_decision_blob_sha1", "source_license", "source_proof_marker",
    "runtime_status", "receiver_repo", "receiver_base_commit", "receiver_branch",
    "target_path", "target_blob_sha1", "receipt_state", "promotion_state",
]


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_SYNEXIA_STRING_FACTS_RECEIPT_FAIL {message}")


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
    if len(data) != len(EXPECTED_MAPPINGS):
        fail(f"expected {len(EXPECTED_MAPPINGS)} rows, got {len(data)}")
    seen: set[str] = set()
    for row in data:
        if len(row) != len(EXPECTED_HEADER):
            fail("column-count drift")
        donor, target, source_path, source_blob, status = row[:5]
        expected = EXPECTED_MAPPINGS.get(donor)
        if expected is None or donor in seen:
            fail(f"mapping coverage drift for {donor}")
        if (target, source_path, source_blob, status) != expected:
            fail(f"mapping identity drift for {donor}")
        if tuple(row[5:19]) != EXPECTED_SOURCE:
            fail(f"source custody drift for {donor}")
        if tuple(row[20:27]) != EXPECTED_RECEIVER:
            fail(f"receiver boundary drift for {donor}")
        if row[19] != "NOT_RUN":
            fail(f"runtime status drift for {donor}")
        seen.add(donor)
    if seen != set(EXPECTED_MAPPINGS):
        fail("mapping set drift")
    print("# proof=" + EXPECTED_PROOF)
    print(EXPECTED_PROOF)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
