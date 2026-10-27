# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Receiver proof must fail closed on stale Java/Catalogue data and forged gates."""
from __future__ import annotations

import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SOURCE = Path(__file__).resolve().parent / "verify_receipt.py"
SPEC = importlib.util.spec_from_file_location("m3jdk21_tree_receipt", SOURCE)
verifier = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(verifier)


class NativePlanReceivingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.receipt = json.loads(verifier.RECEIPT.read_text(encoding="utf-8"))

    def test_actual_source_tree_receipt_is_exact(self) -> None:
        result = verifier.validate()
        self.assertEqual(result["files"], 2)
        self.assertEqual(result["categories"], 8)
        self.assertFalse(result["runtime_qualified"])

    def test_forged_sha1_is_refused(self) -> None:
        owner = self.receipt["files"][0]
        raw = verifier.source_path(verifier.ROOT, owner["path"]).read_bytes()
        with self.assertRaisesRegex(ValueError, "Git postimage blob drift"):
            verifier.verify_digest(raw, "0" * 40, owner["postimage_sha256"])

    def test_forged_sha256_is_refused(self) -> None:
        owner = self.receipt["files"][0]
        raw = verifier.source_path(verifier.ROOT, owner["path"]).read_bytes()
        with self.assertRaisesRegex(ValueError, "SHA-256 postimage drift"):
            verifier.verify_digest(raw, owner["postimage_git_blob_sha1"], "0" * 64)

    def test_duplicate_json_key_is_refused(self) -> None:
        with self.assertRaisesRegex(ValueError, "duplicate receipt JSON key"):
            json.loads('{"schema":"ok","schema":"wrong"}',
                       object_pairs_hook=verifier.exact_json)

    def test_traversal_and_alias_paths_are_refused(self) -> None:
        for path in ("../outside.java", "m3//collections/X.java",
                     "m3/./collections/X.java", "/tmp/outside", "C:/outside",
                     "m3\\collections\\X.java"):
            with self.subTest(path=path), self.assertRaises(ValueError):
                verifier.source_path(verifier.ROOT, path)

    def changed_receipt(self, update) -> Path:
        temp = tempfile.TemporaryDirectory(prefix="m3jdk21-receipt-")
        self.addCleanup(temp.cleanup)
        path = Path(temp.name) / "receipt.json"
        record = copy.deepcopy(self.receipt)
        update(record)
        path.write_text(json.dumps(record), encoding="utf-8")
        return path

    def test_wrong_canonical_source_count_is_refused(self) -> None:
        path = self.changed_receipt(lambda row: row["files"].pop())
        with self.assertRaisesRegex(ValueError, "number of received"):
            verifier.validate(receipt=path)

    def test_forged_catalogue_approval_is_refused(self) -> None:
        path = self.changed_receipt(
            lambda row: row["catalogue"].update(git_blob_sha1="0" * 40))
        with self.assertRaisesRegex(ValueError, "catalogue drift"):
            verifier.validate(receipt=path)

    def test_runtime_promotion_without_gates_is_refused(self) -> None:
        path = self.changed_receipt(
            lambda row: row.update(decision="JDK_RUNTIME_QUALIFIED"))
        with self.assertRaisesRegex(ValueError, "falsely claims"):
            verifier.validate(receipt=path)

    def test_extra_public_java_output_is_refused(self) -> None:
        path = self.changed_receipt(
            lambda row: row["files"].append(copy.deepcopy(row["files"][0])))
        with self.assertRaisesRegex(ValueError, "number of received"):
            verifier.validate(receipt=path)


if __name__ == "__main__":
    unittest.main()
