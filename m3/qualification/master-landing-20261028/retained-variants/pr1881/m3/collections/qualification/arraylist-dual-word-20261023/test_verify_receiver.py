# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Receiver readback/refusal oracles: never touch actual product sources."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_mask_receiver", HERE / "verify_receiver.py")
receiver = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(receiver)


class SourceReceiverContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.receipt = json.loads((receiver.REPO_ROOT / receiver.RECEIPT)
                                 .read_text(encoding="utf-8"))

    def copy_fixture(self, dest: Path):
        paths = ([receiver.RECEIPT.as_posix()]
                 + [item["path"] for item in self.receipt["files"]]
                 + [self.receipt["catalogue"]["path"]]
                 + [item["path"] for item in self.receipt["unchanged_native_sources"]])
        for path in paths:
            source = receiver.REPO_ROOT / path
            target = dest / path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)

    def test_all_actual_postimages_and_jni_are_pinned(self):
        state = receiver.verify(receiver.REPO_ROOT)
        self.assertEqual(state["source_postimages"], 6)
        self.assertEqual(state["catalogue_categories"], 11)
        self.assertEqual(state["unchanged_native_sources"], 2)

    def test_detached_exact_receiving_copy_is_fixed_point(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            self.assertEqual(receiver.verify(root), receiver.verify(root))

    def test_modified_java_implementation_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            source = root / "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java"
            source.write_bytes(source.read_bytes() + b"// drift\n")
            with self.assertRaisesRegex(ValueError, "Git source postimage drift"):
                receiver.verify(root)

    def test_tree_window_owner_mutation_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            source = root / "m3/collections/src/main/java/com/m3/collections/M3TreeWindowPlan.java"
            source.write_bytes(source.read_bytes() + b"// unintended tree change\n")
            with self.assertRaisesRegex(ValueError, "Git source postimage drift"):
                receiver.verify(root)

    def test_missing_tree_regression_test_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            (root / "m3/collections/src/test/java/com/m3/collections/M3TreeCoverageTest.java").unlink()
            with self.assertRaisesRegex(ValueError, "missing/out-of-root"):
                receiver.verify(root)

    def test_unknown_tree_contract_role_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            path = root / receiver.RECEIPT
            receipt = json.loads(path.read_text(encoding="utf-8"))
            tree = next(row for row in receipt["files"] if row["path"].endswith("/M3TreeWindowPlan.java"))
            tree["role"] = "unqualified"
            path.write_text(json.dumps(receipt) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate/unexpected"):
                receiver.verify(root)

    def test_unreviewed_jni_change_is_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            native = root / "m3/collections/src/main/native/collections/m3_index_mask_window.c"
            native.write_bytes(native.read_bytes() + b"/* drift */\n")
            with self.assertRaisesRegex(ValueError, "Git source postimage drift"):
                receiver.verify(root)

    def test_duplicate_receipt_key_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            receipt = root / receiver.RECEIPT
            data = receipt.read_text(encoding="utf-8")
            needle = '"schema": "M3JDK21_COLLECTION_ATOM_RECEIVER_V1",'
            self.assertEqual(data.count(needle), 1)
            receipt.write_text(data.replace(needle, needle + "\n  " + needle),
                               encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate JSON key"):
                receiver.verify(root)

    def test_catalogue_requires_licensed_challenge_reference(self):
        cat = (receiver.REPO_ROOT / self.receipt["catalogue"]["path"]).read_text(
            encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unattributed donor URL"):
            receiver.verify_catalogue(
                cat.replace("https://leetcode.com/problems/move-zeroes/",
                            "http://insecure.example/problems/", 1).encode("utf-8"),
                self.receipt["catalogue"]["category_count"])

    def test_noncanonical_path_and_symlink_refused(self):
        with tempfile.TemporaryDirectory() as home:
            root = Path(home)
            self.copy_fixture(root)
            with self.assertRaisesRegex(ValueError, "noncanonical"):
                receiver.safe_file(root, "m3/collections/../arrays/pom.xml")
            owner = root / "m3/arrays/pom.xml"
            alias = owner.parent / "alias.xml"
            alias.symlink_to(owner)
            with self.assertRaisesRegex(ValueError, "symlink"):
                receiver.safe_file(root, "m3/arrays/alias.xml")


if __name__ == "__main__":
    unittest.main()
