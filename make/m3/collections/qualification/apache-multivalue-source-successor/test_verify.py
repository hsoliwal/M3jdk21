# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Regression and deliberate-refusal tests for the immutable Apache v4 to v5 receipt."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("apache_v5_receiver", HERE / "verify.py")
assert SPEC is not None and SPEC.loader is not None
V5 = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(V5)


class ApacheSourceSuccessorV5Test(unittest.TestCase):
    def test_exact_unchanged_original_v4_replays_before_current_three(self):
        result = V5.verify()
        self.assertEqual("PASS", result["historicV4"])
        self.assertEqual(3, result["changedJavaNativeOwners"])
        self.assertEqual(33, result["originalSourceRows"])

    def test_current_source_drift_is_not_admitted(self):
        receipt = __import__("json").loads(
            V5.read(V5.ROOT,
                    "m3/collections/qualification/apache-multivalue-source-successor/RECEIPT.json"))
        for row in receipt["successors"]:
            real = V5.read(V5.ROOT, row["path"])
            with self.subTest(path=row["path"]):
                with self.assertRaisesRegex(ValueError, "identity drift"):
                    V5.pin(real + b"\n// source drift\n", row["current"], row["path"])

    def test_historical_snapshot_mutation_is_refused(self):
        receipt = __import__("json").loads(
            V5.read(V5.ROOT,
                    "m3/collections/qualification/apache-multivalue-source-successor/RECEIPT.json"))
        for row in receipt["successors"]:
            original = V5.read(V5.ROOT,
                               "m3/collections/qualification/apache-multivalue-source-successor/"
                               + row["snapshot"])
            with self.subTest(path=row["path"]):
                with self.assertRaisesRegex(ValueError, "identity drift"):
                    V5.pin(original + b"\n/* foreign bytes */\n",
                           row["previous"], row["path"])

    def test_unapproved_traversal_paths_and_missing_inputs_refused(self):
        for path in ("../escape", "/etc/passwd",
                     "m3/collections/../../etc/shadow",
                     "m3/collections/qualification/absent-source.java"):
            with self.subTest(path=path):
                with self.assertRaises(ValueError):
                    V5.read(V5.ROOT, path)

    def test_existing_argument_output_is_never_overwritten(self):
        with tempfile.TemporaryDirectory(prefix="m3-apache-v5-refusal-") as scratch:
            target = Path(scratch) / "no-overwrite.args"
            target.write_text("PRESERVE CALLER FILE\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "overwrite source"):
                V5.verify(args_file=target)
            self.assertEqual("PRESERVE CALLER FILE\n",
                             target.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
