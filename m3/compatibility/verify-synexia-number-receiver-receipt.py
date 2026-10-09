#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receipt checker for the Synexia 0..10000 number receiver."""

from __future__ import annotations

import csv
from pathlib import Path

RECEIPT = Path(__file__).with_name("synexia-number-receiver-receipt-20261009.tsv")
EXPECTED_SOURCE = (
    "hsoliwal/com.synexia",
    "7850",
    "d9c6f6e71fe84acdbc8a7f969c2cbbadc8ed6dd9",
    "synexia-m3-recipe/recipes/mindex-number-direct-lookup-current-develop.yaml",
    "a421137641ece4f335462af6c1c887d4c6a0a6fe",
    "synexia-indexstring/docs/MINDEX_NUMBER_DIRECT_LOOKUP_CURRENT_DEVELOP_PROOF_20261002.md",
    "df8116d6e661aaf9c2b28a8e6f1f1f5fb6673c1c",
    "Apache-2.0;OpenJDK-reference-GPL-2.0-only-with-Classpath-exception",
)
EXPECTED_RECEIVER = (
    "529,559",
    "5f68880c6f4a669e2fe82d6a914c071aef4f80f5",
    "codex/m3jdk-number-language-coordinate-20261009",
    "9f964d7edd8cb26cc063c8431f046e3ac7cb84ff",
)
EXPECTED_STATE = "CANDIDATE_RECEIVER_OBSERVED_NO_PROMOTION"
EXPECTED_PROOF = "M3JDK_SYNEXIA_NUMBER_RECEIVER_RECEIPT_PASS rows=9 state=CANDIDATE_RECEIVER_OBSERVED_NO_PROMOTION"
EXPECTED_BLOBS = {
    ".github/workflows/m3-first-release-admission.yml": "52038ad20a680fec46abb8ca5046777801eb1031",
    ".github/workflows/m3-foundation.yml": "b12539c96451bd8ef226639ebebc54551be8cd12",
    "m3/compatibility/check-synexia.py": "c3d1320e52380dc6997c27a6830b69ba18875814",
    "m3/core/src/com/m3/text/M3NumberSpace.java": "7faa5808ca7873698a881b33c35e59fd03cd9e18",
    "m3/core/test/M3LanguageGrammarSupportTest.java": "5eca04f78a981e12304919972481a7e284d71733",
    "m3/core/test/M3NumberSpaceContractTest.java": "923d4ab099450c18a21b232f4cfbc6caa290b3f4",
    "m3/lexicon/synexia-number-target-map.tsv": "907cdfd8a94b60f1711918c3a8e03f2b4cbdf4e4",
    "m3/lexicon/synexia-precompute-field-map.tsv": "a68dd7801dbf56bb30447b8b15e876ea7f6d6fcd",
    "m3/lexicon/synexia-source-manifest.tsv": "3c20aecb021804553fbbccedcb5c5db0d067df29",
}
EXPECTED_HEADER = [
    "source_repo", "source_pr", "source_head", "source_recipe",
    "source_recipe_blob_sha1", "source_proof", "source_proof_blob_sha1",
    "source_license", "receiver_prs", "receiver_base_commit", "receiver_branch",
    "receiver_head", "target_path", "target_blob_sha1", "receipt_state",
    "runtime_dependency", "promotion_state",
]


def fail(message: str) -> None:
    raise SystemExit(f"M3JDK_SYNEXIA_NUMBER_RECEIPT_FAIL {message}")


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
    if len(data) != len(EXPECTED_BLOBS):
        fail(f"expected {len(EXPECTED_BLOBS)} rows, got {len(data)}")
    seen: set[str] = set()
    for row in data:
        if len(row) != len(EXPECTED_HEADER):
            fail("column-count drift")
        (
            source_repo, source_pr, source_head, source_recipe, source_recipe_blob,
            source_proof, source_proof_blob, source_license, receiver_prs,
            receiver_base, receiver_branch, receiver_head, target_path, blob,
            state, runtime_dependency, promotion_state,
        ) = row
        if (
            source_repo, source_pr, source_head, source_recipe, source_recipe_blob,
            source_proof, source_proof_blob, source_license
        ) != EXPECTED_SOURCE:
            fail(f"source custody drift for {target_path}")
        if (receiver_prs, receiver_base, receiver_branch, receiver_head) != EXPECTED_RECEIVER:
            fail(f"receiver custody drift for {target_path}")
        if state != EXPECTED_STATE or runtime_dependency != "none" or promotion_state != "NOT_PROMOTED_HOSTED_GREEN_REQUIRED":
            fail(f"promotion boundary drift for {target_path}")
        if target_path not in EXPECTED_BLOBS or target_path in seen:
            fail(f"target path coverage drift for {target_path}")
        if blob != EXPECTED_BLOBS[target_path]:
            fail(f"target blob drift for {target_path}")
        seen.add(target_path)
    if seen != set(EXPECTED_BLOBS):
        fail("target path set drift")
    print(EXPECTED_PROOF)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
