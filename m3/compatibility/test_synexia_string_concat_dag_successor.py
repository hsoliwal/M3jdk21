#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify exact Synexia concat-DAG checker and jtreg snapshots received by M3JDK."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
RECEIPT_PATH = ROOT / "m3/compatibility/synexia-string-concat-dag-successor-20261013.json"
MAPPING_PATH = ROOT / "m3/docs/name-mapping.json"
EXPECTED_SOURCE = {
    "repository": "hsoliwal/com.synexia",
    "transfers": [
        {
            "pullRequest": 10963,
            "head": "18269f2e186ab151f60270a36ede744ac64bec19",
            "mergeCommit": "e243e183d72bf42d57b2f946e332c8dc49a75ae9",
            "manifestPath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-concat-typed-stringifier-invariant-20261009/manifest.tsv",
            "manifestGitBlob": "9cfa92fb8edc158620fcd0cb1bb40eab052167d3",
            "manifestRow": {"path":"m3/runtime-integration/check-m3string-concat-reference-dag.py","beforeSha256":"80894129e8a66544e27c7c55e06c84da8daae3ea3625015bb71d4e1978ba3b24","afterSha256":"9fd82a96ad813f11c90e593c8cd3b93feae9d5658a7b7b68ca48e95f035b19fa","template":"00-check-m3string-concat-reference-dag.py.txt"},
            "sourceResourcePath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-concat-typed-stringifier-invariant-20261009/00-check-m3string-concat-reference-dag.py.txt",
            "sourceResourceGitBlob": "bddd68cc9b36439fda39fe2fd655b8819fae7e0e",
            "sourceResourceSha256": "9fd82a96ad813f11c90e593c8cd3b93feae9d5658a7b7b68ca48e95f035b19fa",
        },
        {
            "pullRequest": 10970,
            "head": "bfa92488c043a8ad5a95f569858d88e241643fd6",
            "mergeCommit": "48900593efc773183594ac02e9ff0cf1928db197",
            "manifestPath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-concat-reference-dag-jtreg-20261013/manifest.tsv",
            "manifestGitBlob": "37f0e594f1dc06045cd20305c3c06f7a30da27e1",
            "manifestRow": {"path":"test/jdk/java/lang/String/M3StringConcatReferenceDagTest.java","beforeSha256":"111137431d9a475bbc5f9219859f3badd4d138cf3d61e5d1642880abb8175347","afterSha256":"e4624f56a93d68cd04337e39ed2cd3896867149908f287d5839ddff9b965ae95","template":"00-M3StringConcatReferenceDagTest.java.txt"},
            "sourceResourcePath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-concat-reference-dag-jtreg-20261013/00-M3StringConcatReferenceDagTest.java.txt",
            "sourceResourceGitBlob": "edf35d940739581b76b73d7b431989ec4ff0d966",
            "sourceResourceSha256": "e4624f56a93d68cd04337e39ed2cd3896867149908f287d5839ddff9b965ae95",
        },
    ],
}
EXPECTED_CHANGES = [
    {"path":"m3/runtime-integration/check-m3string-concat-reference-dag.py","beforeGitBlob":"b09f011e9f760efc093e467e3b494a903a6527ad","beforeSha256":"80894129e8a66544e27c7c55e06c84da8daae3ea3625015bb71d4e1978ba3b24","afterGitBlob":"bddd68cc9b36439fda39fe2fd655b8819fae7e0e","afterSha256":"9fd82a96ad813f11c90e593c8cd3b93feae9d5658a7b7b68ca48e95f035b19fa"},
    {"path":"test/jdk/java/lang/String/M3StringConcatReferenceDagTest.java","beforeGitBlob":"1c37ce9ae5dc3c13c1d1f10dc35fe6c409033358","beforeSha256":"111137431d9a475bbc5f9219859f3badd4d138cf3d61e5d1642880abb8175347","afterGitBlob":"edf35d940739581b76b73d7b431989ec4ff0d966","afterSha256":"e4624f56a93d68cd04337e39ed2cd3896867149908f287d5839ddff9b965ae95"},
]

def git_blob_sha1(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + bytes([0]) + data).hexdigest()

class SynexiaConcatDagSuccessorTest(unittest.TestCase):
    def test_receipt_binds_merged_sources_and_exact_m3_base(self) -> None:
        receipt = json.loads(RECEIPT_PATH.read_text(encoding="utf-8"))
        self.assertEqual(receipt["schema"], "M3JDK21_SYNX_CONCAT_DAG_RECEIVER_V1")
        self.assertEqual(receipt["scope"], "TOOLING_ONLY_NO_RUNTIME_PROMOTION")
        self.assertEqual(receipt["source"]["repository"], EXPECTED_SOURCE["repository"])
        self.assertEqual(receipt["source"]["transfers"], EXPECTED_SOURCE["transfers"])
        self.assertEqual(receipt["target"]["repository"], "hsoliwal/M3jdk21")
        self.assertEqual(receipt["target"]["baseCommit"], "77035a8ac0448981e0581c736a7db46f0752fe6a")
        self.assertEqual(receipt["target"]["changes"], EXPECTED_CHANGES)
        self.assertEqual(receipt["qualification"]["runtimePromotion"], "BLOCKED")
        self.assertFalse(receipt["change"]["runtimePromotionAuthorized"])

    def test_received_files_are_exact_hash_pinned_synexia_postimages(self) -> None:
        receipt = json.loads(RECEIPT_PATH.read_text(encoding="utf-8"))
        for proof in receipt["target"]["changes"]:
            raw = (ROOT / proof["path"]).read_bytes()
            self.assertEqual(hashlib.sha256(raw).hexdigest(), proof["afterSha256"], proof["path"])
            self.assertEqual(git_blob_sha1(raw), proof["afterGitBlob"], proof["path"])
        checker = (ROOT / "m3/runtime-integration/check-m3string-concat-reference-dag.py").read_text(encoding="utf-8")
        self.assertIn("static String m3Concat(String[] constants, String[] args)", checker)
        self.assertIn("result = m3Append(result, args[index]);", checker)
        self.assertIn("return result == null ? flatConcat(constants, args) : new String(result);", checker)
        self.assertNotIn("static String m3Concat(String[] constants, Object[] args)", checker)
        jtreg = (ROOT / "test/jdk/java/lang/String/M3StringConcatReferenceDagTest.java").read_text(encoding="utf-8")
        self.assertIn('getDeclaredMethod("m3Concat", String[].class, String[].class)', jtreg)
        self.assertIn("String ordered = objectConcat(", jtreg)
        self.assertIn("private static String typedConcat(", jtreg)
        self.assertNotIn("Object[].class", jtreg)
        self.assertNotIn("Object[] args", jtreg)

    def test_name_mapping_preserves_synexia_to_m3_target_identity(self) -> None:
        mapping = json.loads(MAPPING_PATH.read_text(encoding="utf-8"))
        entries = {entry["target"]: entry for entry in mapping["mappings"]}
        for transfer, target, pr, commit, source_blob in [
            (EXPECTED_SOURCE["transfers"][0], "m3/runtime-integration/check-m3string-concat-reference-dag.py", 10963, "e243e183d72bf42d57b2f946e332c8dc49a75ae9", "bddd68cc9b36439fda39fe2fd655b8819fae7e0e"),
            (EXPECTED_SOURCE["transfers"][1], "test/jdk/java/lang/String/M3StringConcatReferenceDagTest.java", 10970, "48900593efc773183594ac02e9ff0cf1928db197", "edf35d940739581b76b73d7b431989ec4ff0d966"),
        ]:
            entry = entries[target]
            self.assertEqual(entry["source"], transfer["sourceResourcePath"])
            self.assertEqual(entry["source_commit"], commit)
            self.assertEqual(entry["lineage"]["donor_pr"], pr)
            self.assertEqual(entry["lineage"]["source_resource_git_blob"], source_blob)
            self.assertEqual(entry["lineage"]["donor_repo"], "hsoliwal/com.synexia")

    def test_pending_status_never_claims_runtime_qualification(self) -> None:
        receipt = json.loads(RECEIPT_PATH.read_text(encoding="utf-8"))
        self.assertEqual(receipt["qualification"]["receiverProof"], "PENDING_CI")
        self.assertEqual(receipt["qualification"]["concatChecker"], "PENDING_CI")
        self.assertEqual(receipt["qualification"]["javaBaseBuild"], "NOT_RUN")
        self.assertEqual(receipt["qualification"]["jtreg"], "PENDING_CI")

if __name__ == "__main__":
    unittest.main(verbosity=2)
