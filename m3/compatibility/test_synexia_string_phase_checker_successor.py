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


class SynexiaStringPhaseCheckerSuccessorTest(unittest.TestCase):
    def test_receipt_is_exact_and_non_promoting(self):
        receipt = json.loads(RECEIPT_PATH.read_text(encoding="utf-8"))
        self.assertEqual(EXPECTED_RECEIPT, receipt)
        self.assertEqual("TOOLING_ONLY_NO_RUNTIME_PROMOTION", receipt["scope"])
        self.assertFalse(receipt["change"]["runtimeProductFilesChanged"])
        self.assertFalse(receipt["change"]["runtimePromotionAuthorized"])

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
        validate_target(TARGET_PATH.read_bytes())

    def test_checker_drift_fails_closed(self):
        data = TARGET_PATH.read_bytes()
        changed = data.replace(NEW_MARKER.encode("utf-8"),
                               b"return concatUnbalanced(left, right);", 1)
        self.assertNotEqual(data, changed)
        with self.assertRaisesRegex(ValueError, "SHA-256 differs"):
            validate_target(changed)


if __name__ == "__main__":
    unittest.main()
