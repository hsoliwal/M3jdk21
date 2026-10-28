# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Deterministic positive/refusal regression tests for the read-only receiver."""
from __future__ import annotations
from copy import deepcopy
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_current_mask_receiver", HERE / "verify_receiver.py")
receiver = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(receiver)


class ReceiverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.receipt = json.loads((HERE / "RECEIVER_RECEIPT.json").read_text(encoding="utf-8"))

    def test_exact_current_checkout_receipt(self):
        facts = receiver.verify(receiver.ROOT, self.receipt)
        self.assertEqual(facts, {"files": 3, "native": 2, "categories": 11})

    def test_runtime_promotion_refused(self):
        changed = deepcopy(self.receipt)
        changed["state"] = "READY_TO_MERGE"
        with self.assertRaisesRegex(ValueError, "runtime promotion"):
            receiver.verify(receiver.ROOT, changed)

    def test_missing_or_duplicate_java_postimage_refused(self):
        altered = deepcopy(self.receipt)
        altered["source_postimages"][1] = dict(altered["source_postimages"][0])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            receiver.verify(receiver.ROOT, altered)

    def test_unqualified_jtreg_receipt_refused(self):
        altered = deepcopy(self.receipt)
        altered["evidence"]["source_built_jtreg_abi_gc_jmm"] = "PASSED_UNPROVEN"
        with self.assertRaisesRegex(ValueError, "jtreg"):
            receiver.verify(receiver.ROOT, altered)

    def test_jni_source_pin_refused(self):
        altered = deepcopy(self.receipt)
        altered["native_unmodified"][0]["git_blob"] = "0" * 40
        with self.assertRaisesRegex(ValueError, "JNI"):
            receiver.verify(receiver.ROOT, altered)

    def test_wrong_algorithm_custody_refused(self):
        altered = deepcopy(self.receipt)
        altered["catalogue"]["category_count"] = 10
        with self.assertRaisesRegex(ValueError, "catalogue"):
            receiver.verify(receiver.ROOT, altered)

    def test_modified_source_byte_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            paths = set(receiver.FILES) | set(receiver.NATIVE) | {receiver.CATALOGUE}
            for path in paths:
                source = receiver.ROOT / path
                destination = root / path
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, destination)
            changed = root / next(iter(receiver.FILES))
            changed.write_bytes(changed.read_bytes() + b"\n// source drift\n")
            with self.assertRaisesRegex(ValueError, "source hash drift"):
                receiver.verify(root, self.receipt)

    def test_symlinked_source_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "real").write_text("contents", encoding="utf-8")
            (root / "link").symlink_to(root / "real")
            with self.assertRaisesRegex(ValueError, "symlink"):
                receiver.source(root, "link")


if __name__ == "__main__":
    unittest.main()
