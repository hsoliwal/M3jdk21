# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Receiver mutation/refusal oracles; all mutations are isolated temp copies."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("m3_tree_commit_receiver", HERE / "verify_receipt.py")
receiver = importlib.util.module_from_spec(spec)
spec.loader.exec_module(receiver)


class TreeCommitReceiverTests(unittest.TestCase):
    def stage(self, root: Path):
        for name in (receiver.RECEIPT, receiver.PARENT, receiver.SOURCE,
                     receiver.TEST, receiver.CATALOGUE):
            src = receiver.ROOT / name
            dst = root / name
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(src, dst)

    def test_current_source_and_history_are_sealed(self):
        value = receiver.verify(receiver.ROOT)
        self.assertEqual(value["postimages"], 2)
        self.assertEqual(value["categories"], 4)
        self.assertEqual(value["parent_source_history"], 1)

    def test_identical_detached_copy_is_fixed_point(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            first = receiver.verify(root)
            self.assertEqual(first, receiver.verify(root))

    def test_source_drift_is_refused_before_runtime_promotion(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            path = root / receiver.SOURCE
            path.write_bytes(path.read_bytes() + b"// outside reviewed source\n")
            with self.assertRaisesRegex(ValueError, "postimage drift"):
                receiver.verify(root)

    def test_additive_junit_test_drift_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            path = root / receiver.TEST
            path.write_text("unreviewed", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "postimage drift"):
                receiver.verify(root)

    def test_prior_receiver_lineage_change_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            path = root / receiver.PARENT
            text = path.read_text(encoding="utf-8")
            path.write_text(text + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "historical receiving receipt drift"):
                receiver.verify(root)

    def test_duplicate_receipt_key_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            path = root / receiver.RECEIPT
            text = path.read_text(encoding="utf-8")
            needle = '"schema": "M3JDK21_TREE_NATIVE_COMMIT_ATOMIC_RECEIVER_V1",'
            self.assertEqual(text.count(needle), 1)
            path.write_text(text.replace(needle, needle + "\n  " + needle),
                            encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate receiver JSON key"):
                receiver.verify(root)

    def test_symlinked_product_source_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            path = root / receiver.SOURCE
            alternate = path.with_name("source-copy.java")
            path.rename(alternate)
            path.symlink_to(alternate)
            with self.assertRaisesRegex(ValueError, "symlink"):
                receiver.verify(root)

    def test_catalogue_mutation_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.stage(root)
            path = root / receiver.CATALOGUE
            path.write_text(path.read_text(encoding="utf-8") + "# unexpected\n",
                            encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "catalogue source drift"):
                receiver.verify(root)


if __name__ == "__main__":
    unittest.main()
