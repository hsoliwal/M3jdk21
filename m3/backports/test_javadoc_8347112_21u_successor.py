# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Exact JDK21u-merge Javadoc successor: no generic hash or backport approval."""
import hashlib
import unittest
from pathlib import Path

import verify


class Javadoc8347112SuccessorTest(unittest.TestCase):
    def setUp(self):
        self.root = Path(__file__).resolve().parents[2]
        self.target = self.root / verify.JDK_8347112_HANDLER_PATH
        self.data = self.target.read_bytes()
        self.digest = hashlib.sha256(self.data).hexdigest()
        self.row = {
            "target_path": verify.JDK_8347112_HANDLER_PATH,
            "target_sha256": verify.JDK_8347112_HANDLER_HISTORICAL_SHA256,
        }

    def test_exact_post_21u_merge_source_is_recognized(self):
        self.assertEqual(verify.JDK_8347112_HANDLER_21U_SHA256, self.digest)
        self.assertEqual(verify.JDK_8347112_HANDLER_21U_GIT_BLOB,
                         verify.git_blob_sha1(self.data))
        self.assertTrue(verify._verified_javadoc_21u_successor(
            self.row, self.digest, self.data
        ))

    def test_complete_five_file_backport_validator_accepts_successor(self):
        verify.verify_javadoc_8347112_backport(self.root)

    def test_unrelated_target_paths_are_refused(self):
        changed = dict(self.row, target_path="src/java.base/share/classes/java/lang/String.java")
        self.assertFalse(verify._verified_javadoc_21u_successor(
            changed, self.digest, self.data
        ))

    def test_rewritten_historical_adaptation_hash_is_refused(self):
        changed = dict(self.row, target_sha256="0" * 64)
        self.assertFalse(verify._verified_javadoc_21u_successor(
            changed, self.digest, self.data
        ))

    def test_unrecognized_successor_hash_is_refused(self):
        self.assertFalse(verify._verified_javadoc_21u_successor(
            self.row, "0" * 64, self.data
        ))

    def test_modified_bytes_are_refused_even_with_expected_digest(self):
        self.assertFalse(verify._verified_javadoc_21u_successor(
            self.row, self.digest, self.data + b"// unexpected drift\n"
        ))

    def test_original_recursion_and_21u_override_both_present(self):
        self.assertIn(
            b"if (!configuration.shouldExcludeDocFileDir(srcfile.getName()))",
            self.data,
        )
        self.assertIn(b"public boolean isIndexable()", self.data)
        self.assertNotIn(b"options.copyDocfileSubdirs()", self.data)


if __name__ == "__main__":
    unittest.main()
