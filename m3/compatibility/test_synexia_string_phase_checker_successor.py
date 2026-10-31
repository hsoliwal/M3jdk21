#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Prove the exact Synexia-owned checker successor received by M3JDK21."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
RECEIPT_PATH = ROOT / "m3/compatibility/synexia-string-phase-checker-successor-20261013.json"
RECEIPT_V2_PATH = ROOT / "m3/compatibility/synexia-string-phase-checker-successor-20261015-v2.json"
RECEIPT_V3_PATH = ROOT / "m3/compatibility/synexia-string-phase-checker-successor-20261028-v3.json"
TARGET_PATH = ROOT / "m3/synexia-import/current-full-borrow/check_string_phase.py"

OLD_MARKER = "M3StringTuple tuple = new M3StringTuple(left, right);"
NEW_MARKER = "return concatBalanced(left, right);"
EXPECTED_MANIFEST = (
    "# path\tbefore\tafter\ttemplate\n"
    ".github/workflows/mindex-string-backing.yml\t33cefc4ff5aad4b2eb74532261b06daf6d21311f48e9a154df5619e3b8f3ce10\t927ba49c51709896a4610d89daf4d669217e27713153cd46f863aafb0dd63989\t00-mindex-string-backing.yml.txt\n"
    "m3/runtime-integration/check-m3string-concat-overflow-current.py\tABSENT\te16460bd56377ca59c395e3336dad5914cab1d46c8423b22ee7a0c4366e38cf8\t01-check-m3string-concat-overflow-current.py.txt\n"
    "m3/runtime-integration/test_m3string_concat_overflow_current.py\tABSENT\t5a28b85c7f6e5af794acf6663d32ac5a5c82d31e67fc6f22e1a1dc688925eab4\t02-test_m3string_concat_overflow_current.py.txt\n"
    "m3/synexia-import/current-full-borrow/check_string_phase.py\t892a5986d7c8a24909cbe04c29a0a44dc22eea508463a3bef8bd3e8444a6304c\t11613c443875536088a13eb932c4142f723d3d1366baf32333accc37260c8fc4\t03-check_string_phase.py.txt\n"
)
EXPECTED_RECEIPT = json.loads("{\n  \"schema\": \"M3JDK21_STRING_PHASE_CHECKER_SUCCESSOR_V1\",\n  \"scope\": \"TOOLING_ONLY_NO_RUNTIME_PROMOTION\",\n  \"source\": {\n    \"repository\": \"hsoliwal/com.synexia\",\n    \"pullRequest\": 10942,\n    \"head\": \"192607fed74e6ff27beaf553db6ec169afc40b3d\",\n    \"mergeCommit\": \"b6b1af473ea8ca5073b0bd3b16a7a0063db1782d\",\n    \"manifestPath\": \"synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-concat-current-proof-20261012/manifest.tsv\",\n    \"manifestGitBlob\": \"101be05c8b045749e5d1e86a97b73bd4ae1c4bd1\",\n    \"sourceResourcePath\": \"synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-concat-current-proof-20261012/03-check_string_phase.py.txt\",\n    \"sourceResourceGitBlob\": \"9a1d8576f4b8e5f96c691a3a1b25c3bd77fa5c02\",\n    \"sourceResourceSha256\": \"11613c443875536088a13eb932c4142f723d3d1366baf32333accc37260c8fc4\"\n  },\n  \"target\": {\n    \"repository\": \"hsoliwal/M3jdk21\",\n    \"baseCommit\": \"ffc16b832c49798c422ded0d6ddbc5dbe524cce8\",\n    \"path\": \"m3/synexia-import/current-full-borrow/check_string_phase.py\",\n    \"beforeGitBlob\": \"67d562b0a2a72bd72a185007b2345b87fb3a8e68\",\n    \"beforeSha256\": \"892a5986d7c8a24909cbe04c29a0a44dc22eea508463a3bef8bd3e8444a6304c\",\n    \"afterGitBlob\": \"9a1d8576f4b8e5f96c691a3a1b25c3bd77fa5c02\",\n    \"afterSha256\": \"11613c443875536088a13eb932c4142f723d3d1366baf32333accc37260c8fc4\"\n  },\n  \"change\": {\n    \"oldMarker\": \"M3StringTuple tuple = new M3StringTuple(left, right);\",\n    \"newMarker\": \"return concatBalanced(left, right);\",\n    \"expectedOccurrences\": 1,\n    \"runtimeProductFilesChanged\": false,\n    \"runtimePromotionAuthorized\": false\n  }\n}\n")

EXPECTED_RECEIPT_V2 = json.loads(r'''{
  "schema": "M3JDK21_STRING_PHASE_CHECKER_SUCCESSOR_V2",
  "scope": "TOOLING_ONLY_NO_RUNTIME_PROMOTION",
  "predecessor": {
    "receiptPath": "m3/compatibility/synexia-string-phase-checker-successor-20261013.json",
    "receiptGitBlob": "a0373fd42a1fe2b8104e3675abc1919254950fff",
    "receiptSha256": "78fc05a0cb9b7f26e9a6226b2406cc38801f09040dc14e445e31651f7a9385af",
    "targetAfterGitBlob": "9a1d8576f4b8e5f96c691a3a1b25c3bd77fa5c02",
    "targetAfterSha256": "11613c443875536088a13eb932c4142f723d3d1366baf32333accc37260c8fc4"
  },
  "source": {
    "repository": "hsoliwal/com.synexia",
    "pullRequest": 11615,
    "headAtCapture": "90fc480bcd16aa044b103d4fff84f3d554553a83",
    "stateAtCapture": "OPEN_DRAFT",
    "manifestPath": "synexia-openrewrite-recipes/recipes/m3-string-successor-chain-20261014/manifest.tsv",
    "manifestGitBlob": "19ac98444b295f327b22d54f2eb1439ff25efbc3",
    "sourceResourcePath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-string-successor-chain/check_string_phase.py",
    "sourceResourceGitBlob": "274753ece4a5cfd11dc2e98feca50f2601540b8d",
    "sourceResourceSha256": "bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3"
  },
  "target": {
    "repository": "hsoliwal/M3jdk21",
    "baseCommit": "ed0d5435eb8796cfd4c9375edfc05b72bb16c8c6",
    "path": "m3/synexia-import/current-full-borrow/check_string_phase.py",
    "beforeGitBlob": "9a1d8576f4b8e5f96c691a3a1b25c3bd77fa5c02",
    "beforeSha256": "11613c443875536088a13eb932c4142f723d3d1366baf32333accc37260c8fc4",
    "afterGitBlob": "274753ece4a5cfd11dc2e98feca50f2601540b8d",
    "afterSha256": "bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3"
  },
  "change": {
    "relation": "ADDITIVE_SOURCE_OBSERVATION_V2",
    "v1HistoryPreserved": true,
    "runtimeProductFilesChanged": false,
    "runtimePromotionAuthorized": false,
    "runtimeAcceptance": "NOT_CLAIMED",
    "performanceEvidence": "NOT_MEASURED"
  }
}''')



EXPECTED_RECEIPT_V3 = json.loads(r'''{
  "schema": "M3JDK21_STRING_PHASE_CHECKER_SUCCESSOR_V3",
  "scope": "TOOLING_ONLY_NO_RUNTIME_PROMOTION",
  "predecessor": {
    "receiptPath": "m3/compatibility/synexia-string-phase-checker-successor-20261015-v2.json",
    "receiptGitBlob": "52cacd20c406f57027734d2e78b405c8b6176e9f",
    "receiptSha256": "e47b404b87e9748f323cfbca53f07fcde3c3d1a5b14f221e14df6a92613cb1b2",
    "targetAfterGitBlob": "274753ece4a5cfd11dc2e98feca50f2601540b8d",
    "targetAfterSha256": "bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3"
  },
  "source": {
    "repository": "hsoliwal/com.synexia",
    "pullRequest": 13719,
    "headAtCapture": "842be10c3989ca74b51f43c17bc3e1c0f3728e19",
    "stateAtCapture": "OPEN_DRAFT",
    "recipe": "com.synexia.rewrite.M3Jdk21StringAdditionalOwnerSuccessors20261027",
    "manifestPath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-additional-owner-successors-20261027/manifest.tsv",
    "manifestGitBlob": "42d80eb260aa972654053eb79bd58e58f885eff4",
    "manifestSha256": "29a06353c8f603f522ab3048cca155c0d74be5b1bf316be687772c25dff43efd",
    "sourceResourcePath": "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-additional-owner-successors-20261027/check_string_phase.py.after.txt",
    "sourceResourceGitBlob": "02a0276a37f7e44fb520b82e79a5b0e2553d9a5b",
    "sourceResourceSha256": "492b862c07eb39809c8dd3ac7dddcba5cbf9ad73b82a9cf52740d0df53441dda"
  },
  "target": {
    "repository": "hsoliwal/M3jdk21",
    "observedHead": "8ce34942adce886e402a395f015760be200e31ed",
    "path": "m3/synexia-import/current-full-borrow/check_string_phase.py",
    "afterGitBlob": "02a0276a37f7e44fb520b82e79a5b0e2553d9a5b",
    "afterSha256": "492b862c07eb39809c8dd3ac7dddcba5cbf9ad73b82a9cf52740d0df53441dda"
  },
  "change": {
    "relation": "QUALIFIED_CANONICAL_CURRENT_SOURCE_RECEIPT",
    "v1AndV2HistoryPreserved": true,
    "earlierV2ToRecipeEpochHistory": "SOURCE_EPOCH_OBSERVED_NOT_SEMANTICALLY_RECONSTRUCTED",
    "runtimeProductFilesChanged": false,
    "runtimePromotionAuthorized": false,
    "runtimeAcceptance": "NOT_ACCEPTED",
    "performanceEvidence": "NOT_MEASURED"
  }
}
''')


def git_blob(data: bytes) -> str:
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def validate_target(data: bytes) -> None:
    if hashlib.sha256(data).hexdigest() != EXPECTED_RECEIPT["target"]["afterSha256"]:
        raise ValueError("checker SHA-256 differs from pinned Synexia postimage")
    if git_blob(data) != EXPECTED_RECEIPT["target"]["afterGitBlob"]:
        raise ValueError("checker Git blob differs from pinned Synexia postimage")
    text = data.decode("utf-8")
    if text.count(NEW_MARKER) != 1 or OLD_MARKER in text:
        raise ValueError("checker does not contain the exact balanced-concat successor")

def validate_target_v2(data: bytes) -> None:
    if hashlib.sha256(data).hexdigest() != EXPECTED_RECEIPT_V2["target"]["afterSha256"]:
        raise ValueError("checker SHA-256 differs from pinned Synexia V2 postimage")
    if git_blob(data) != EXPECTED_RECEIPT_V2["target"]["afterGitBlob"]:
        raise ValueError("checker Git blob differs from pinned Synexia V2 postimage")


def validate_receipt_v3(receipt: dict, predecessor: bytes) -> None:
    if receipt != EXPECTED_RECEIPT_V3:
        raise ValueError("unreviewed checker V3 receipt")
    expected = receipt["predecessor"]
    if (hashlib.sha256(predecessor).hexdigest() != expected["receiptSha256"]
            or git_blob(predecessor) != expected["receiptGitBlob"]):
        raise ValueError("checker V3 predecessor receipt drift")


def validate_target_v3(data: bytes, receipt: dict, predecessor: bytes) -> None:
    validate_receipt_v3(receipt, predecessor)
    if hashlib.sha256(data).hexdigest() != receipt["target"]["afterSha256"]:
        raise ValueError("checker SHA-256 differs from pinned Synexia V3 postimage")
    if git_blob(data) != receipt["target"]["afterGitBlob"]:
        raise ValueError("checker Git blob differs from pinned Synexia V3 postimage")


class SynexiaStringPhaseCheckerSuccessorTest(unittest.TestCase):
    def test_receipt_is_exact_and_non_promoting(self):
        receipt = json.loads(RECEIPT_PATH.read_text(encoding="utf-8"))
        self.assertEqual(EXPECTED_RECEIPT, receipt)
        self.assertEqual("TOOLING_ONLY_NO_RUNTIME_PROMOTION", receipt["scope"])
        self.assertFalse(receipt["change"]["runtimeProductFilesChanged"])
        self.assertFalse(receipt["change"]["runtimePromotionAuthorized"])

    def test_v2_receipt_extends_v1_without_runtime_promotion(self):
        v1_bytes = RECEIPT_PATH.read_bytes()
        v1 = json.loads(v1_bytes)
        self.assertEqual(EXPECTED_RECEIPT, v1)
        v2 = json.loads(RECEIPT_V2_PATH.read_text(encoding="utf-8"))
        self.assertEqual(EXPECTED_RECEIPT_V2, v2)
        self.assertEqual(hashlib.sha256(v1_bytes).hexdigest(),
                         v2["predecessor"]["receiptSha256"])
        self.assertEqual(git_blob(v1_bytes), v2["predecessor"]["receiptGitBlob"])
        self.assertEqual(v1["target"]["afterGitBlob"],
                         v2["predecessor"]["targetAfterGitBlob"])
        self.assertEqual(v1["target"]["afterSha256"],
                         v2["predecessor"]["targetAfterSha256"])
        self.assertEqual(v1["target"]["afterGitBlob"],
                         v2["target"]["beforeGitBlob"])
        self.assertEqual(v1["target"]["afterSha256"],
                         v2["target"]["beforeSha256"])
        self.assertEqual(v2["source"]["sourceResourceGitBlob"],
                         v2["target"]["afterGitBlob"])
        self.assertEqual(v2["source"]["sourceResourceSha256"],
                         v2["target"]["afterSha256"])
        self.assertEqual("OPEN_DRAFT", v2["source"]["stateAtCapture"])
        self.assertFalse(v2["change"]["runtimeProductFilesChanged"])
        self.assertFalse(v2["change"]["runtimePromotionAuthorized"])
        self.assertEqual("NOT_MEASURED", v2["change"]["performanceEvidence"])

    def test_source_manifest_and_target_postimage_are_cross_bound(self):
        source = EXPECTED_RECEIPT["source"]
        target = EXPECTED_RECEIPT["target"]
        self.assertEqual(source["sourceResourceSha256"], target["afterSha256"])
        self.assertEqual(source["sourceResourceGitBlob"], target["afterGitBlob"])
        manifest = EXPECTED_MANIFEST.encode("utf-8")
        self.assertEqual(git_blob(manifest), source["manifestGitBlob"])
        expected_row = "\t".join((
            target["path"], target["beforeSha256"], target["afterSha256"],
            "03-check_string_phase.py.txt",
        ))
        rows = [line for line in EXPECTED_MANIFEST.splitlines()
                if line.startswith(target["path"] + "\t")]
        self.assertEqual([expected_row], rows)

    def test_target_matches_synexia_postimage(self):
        validate_target_v3(TARGET_PATH.read_bytes(),
                           json.loads(RECEIPT_V3_PATH.read_text(encoding="utf-8")),
                           RECEIPT_V2_PATH.read_bytes())

    def test_checker_drift_fails_closed(self):
        data = TARGET_PATH.read_bytes()
        changed = data.replace(b"EXPECTED_PRECOMPUTE_OBSERVATION",
                               b"EXPECTED_PRECOMPUTE_OBSERVATION_DRIFT", 1)
        self.assertNotEqual(data, changed)
        with self.assertRaisesRegex(ValueError, "SHA-256 differs"):
            validate_target_v3(changed,
                               json.loads(RECEIPT_V3_PATH.read_text(encoding="utf-8")),
                               RECEIPT_V2_PATH.read_bytes())

    def test_v3_receipt_extends_exact_v2_without_runtime_promotion(self):
        predecessor = RECEIPT_V2_PATH.read_bytes()
        receipt = json.loads(RECEIPT_V3_PATH.read_text(encoding="utf-8"))
        validate_receipt_v3(receipt, predecessor)
        v2 = json.loads(predecessor)
        self.assertEqual(EXPECTED_RECEIPT_V2, v2)
        self.assertEqual(v2["target"]["afterGitBlob"],
                         receipt["predecessor"]["targetAfterGitBlob"])
        self.assertEqual(v2["target"]["afterSha256"],
                         receipt["predecessor"]["targetAfterSha256"])
        self.assertEqual(receipt["source"]["sourceResourceGitBlob"],
                         receipt["target"]["afterGitBlob"])
        self.assertEqual(receipt["source"]["sourceResourceSha256"],
                         receipt["target"]["afterSha256"])
        self.assertFalse(receipt["change"]["runtimeProductFilesChanged"])
        self.assertFalse(receipt["change"]["runtimePromotionAuthorized"])
        self.assertEqual("NOT_ACCEPTED", receipt["change"]["runtimeAcceptance"])
        self.assertEqual("NOT_MEASURED", receipt["change"]["performanceEvidence"])
        self.assertEqual("SOURCE_EPOCH_OBSERVED_NOT_SEMANTICALLY_RECONSTRUCTED",
                         receipt["change"]["earlierV2ToRecipeEpochHistory"])

    def test_v3_receipt_field_and_predecessor_drift_refuse(self):
        predecessor = RECEIPT_V2_PATH.read_bytes()
        good = json.loads(RECEIPT_V3_PATH.read_text(encoding="utf-8"))
        self.assertEqual(EXPECTED_RECEIPT_V3, good)
        def leaves(value, path=()):
            for key, item in value.items():
                if isinstance(item, dict):
                    yield from leaves(item, path + (key,))
                else:
                    yield path + (key,), item
        for path, value in leaves(good):
            changed = json.loads(json.dumps(good))
            owner = changed
            for key in path[:-1]:
                owner = owner[key]
            owner[path[-1]] = (not value if isinstance(value, bool)
                               else value + 1 if isinstance(value, int)
                               else value + "-drift")
            with self.subTest(path=path):
                with self.assertRaisesRegex(ValueError, "unreviewed checker V3 receipt"):
                    validate_receipt_v3(changed, predecessor)
        for changed in ({}, {**good, "unexpected": "unsealed"}):
            with self.assertRaisesRegex(ValueError, "unreviewed checker V3 receipt"):
                validate_receipt_v3(changed, predecessor)
        with self.assertRaisesRegex(ValueError, "checker V3 predecessor receipt drift"):
            validate_receipt_v3(good, predecessor + b"\n")



if __name__ == "__main__":
    unittest.main()
