# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Positive and refusal cases for the source-sealed target-only receiving gate."""
from __future__ import annotations

import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_current_arraymask_receiver", HERE / "verify.py")
receiver = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(receiver)
ROOT = HERE.parents[3]


def receipt() -> dict:
    return json.loads((HERE / "RECEIVER_RECEIPT.json").read_text(encoding="utf-8"))


class ReceiverQualificationTests(unittest.TestCase):
    def test_exact_current_checkout_is_pinned_without_runtime_promotion(self):
        summary = receiver.verify_receipt(ROOT, receipt())
        self.assertEqual(summary["sealed_java_and_pom"], 3)
        self.assertEqual(summary["unchanged_native_sources"], 2)
        self.assertEqual(summary["catalogue_categories"], 11)
        self.assertEqual(summary["runtime_promotion"], "BLOCKED")

    def test_unrecorded_postimage_is_refused(self):
        mutated = copy.deepcopy(receipt())
        mutated["files"][0]["postimage_blob"] = "0" * 40
        with self.assertRaisesRegex(ValueError, "Git/SHA256 drift"):
            receiver.verify_receipt(ROOT, mutated)

    def test_unknown_added_source_is_refused(self):
        mutated = copy.deepcopy(receipt())
        mutated["files"][0]["path"] = "src/java.base/share/classes/java/util/ArrayList.java"
        with self.assertRaisesRegex(ValueError, "unapproved"):
            receiver.verify_receipt(ROOT, mutated)

    def test_duplicate_target_file_is_refused(self):
        mutated = copy.deepcopy(receipt())
        mutated["files"][1]["path"] = mutated["files"][0]["path"]
        with self.assertRaisesRegex(ValueError, "duplicate"):
            receiver.verify_receipt(ROOT, mutated)

    def test_unqualified_runtime_claim_is_refused(self):
        mutated = copy.deepcopy(receipt())
        mutated["gates"]["source_built_M3JDK21_jtreg"] = "PASS_NO_JTREG_PROOF"
        with self.assertRaisesRegex(ValueError, "unqualified release"):
            receiver.verify_receipt(ROOT, mutated)

    def test_native_source_drift_is_refused(self):
        mutated = copy.deepcopy(receipt())
        mutated["unchanged_native"][0]["git_blob"] = "f" * 40
        with self.assertRaisesRegex(ValueError, "JNI source mutated"):
            receiver.verify_receipt(ROOT, mutated)

    def test_catalogue_sha_drift_is_refused(self):
        mutated = copy.deepcopy(receipt())
        mutated["catalogue"]["git_blob"] = "f" * 40
        with self.assertRaisesRegex(ValueError, "byte-pinned"):
            receiver.verify_receipt(ROOT, mutated)

    def test_ambiguous_paths_and_symlinks_are_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "src").mkdir()
            (root / "src" / "owned.java").write_text("class Owned {}")
            (root / "linked").symlink_to(root / "src", target_is_directory=True)
            for candidate in ("../bad.java", "src//owned.java", "src/./owned.java",
                              "/etc/passwd", "C:/bad.java", "linked/owned.java"):
                with self.subTest(candidate=candidate):
                    with self.assertRaises(ValueError):
                        receiver.source_path(root, candidate)


if __name__ == "__main__":
    unittest.main()
