#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Adversarial current/historical admission checks in isolated small fixtures."""

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest

sys.dont_write_bytecode = True
SPEC = importlib.util.spec_from_file_location("array_mask_successor", Path(__file__).with_name("verify_successor.py"))
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


def snapshot(root):
    result = {}
    for path in root.rglob("*"):
        info = path.lstat()
        if path.is_symlink():
            item = ("symlink", os.readlink(path), info.st_mode, info.st_mtime_ns)
        elif path.is_file():
            item = ("file", hashlib.sha256(path.read_bytes()).hexdigest(), info.st_mode, info.st_mtime_ns)
        else:
            continue
        result[path.relative_to(root).as_posix()] = item
    return result


class AdmissionContract(unittest.TestCase):
    inputs = None
    output = None

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="case-", dir=self.output)
        self.root = Path(self.temporary.name) / "receiver"
        self.root.mkdir()
        for relative, data in self.inputs.items():
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)

    def tearDown(self):
        self.temporary.cleanup()

    def refuses(self, signature, family="all"):
        before = snapshot(self.root)
        with self.assertRaisesRegex(ValueError, signature):
            VERIFY.check(self.root, family)
        self.assertEqual(before, snapshot(self.root), "admission must be read-only")

    def test_exact_current_and_history_are_separate(self):
        before = snapshot(self.root)
        result = VERIFY.check(self.root)
        self.assertEqual((4, 2, 11), (len(result["current"]), len(result["unchanged"]), len(result["historical"])))
        self.assertEqual(before, snapshot(self.root))

    def test_old_postimage_is_not_current_admission(self):
        relative = next(iter(VERIFY.BITMAP_OUTPUTS))
        template = VERIFY.BITMAP + "/src/main/resources/templates/" + Path(relative).name
        (self.root / relative).write_bytes(self.inputs[template])
        self.refuses("REFUSE_CURRENT_DRIFT")

    def test_each_current_source_drift_refuses(self):
        current = list(VERIFY.check(self.root)["current"])
        for relative in current:
            with self.subTest(path=relative):
                target = self.root / relative
                original = target.read_bytes()
                target.write_bytes(original + b"\n// unadmitted drift\n")
                self.refuses("REFUSE_CURRENT_DRIFT")
                target.write_bytes(original)

    def test_each_historical_source_drift_refuses(self):
        historical = list(VERIFY.check(self.root)["historical"])
        for relative in historical:
            with self.subTest(path=relative):
                target = self.root / relative
                original = target.read_bytes()
                target.write_bytes(original + b"\n# changed historical claim\n")
                self.refuses("REFUSE_HISTORICAL_DRIFT")
                target.write_bytes(original)

    def test_unchanged_histogram_owners_remain_gates(self):
        unchanged = list(VERIFY.check(self.root)["unchanged"])
        for relative in unchanged:
            with self.subTest(path=relative):
                target = self.root / relative
                original = target.read_bytes()
                target.write_bytes(original + b"\n// unadmitted owner\n")
                self.refuses("REFUSE_CURRENT_DRIFT")
                target.write_bytes(original)

    def test_successor_manifest_tamper_refuses(self):
        target = self.root / VERIFY.QUALIFICATION / "SUCCESSOR.tsv"
        target.write_bytes(target.read_bytes() + b"# unsigned admission change\n")
        self.refuses("REFUSE_SUCCESSOR_MANIFEST_DRIFT")

    def test_missing_current_source_refuses(self):
        (self.root / next(iter(VERIFY.BITMAP_OUTPUTS))).unlink()
        self.refuses("REFUSE_MISSING_OR_NONREGULAR")

    def test_symlink_source_refuses(self):
        target = self.root / next(iter(VERIFY.BITMAP_OUTPUTS))
        target.unlink()
        target.symlink_to(self.root / VERIFY.QUALIFICATION / "SUCCESSOR.tsv")
        self.refuses("REFUSE_SYMLINK")

    def test_symlink_source_parent_refuses(self):
        parent = self.root / "m3/collections/src/main/native/collections"
        saved = parent.with_name("saved-native")
        parent.rename(saved)
        parent.symlink_to(saved, target_is_directory=True)
        self.refuses("REFUSE_SYMLINK")

    def test_source_directory_refuses(self):
        target = self.root / next(iter(VERIFY.BITMAP_OUTPUTS))
        target.unlink()
        target.mkdir()
        self.refuses("REFUSE_MISSING_OR_NONREGULAR")

    def test_family_scope_retains_its_own_exact_gates(self):
        (self.root / next(iter(VERIFY.BITMAP_OUTPUTS))).unlink()
        result = VERIFY.check(self.root, "histogram")
        self.assertEqual((2, 2, 7), (len(result["current"]), len(result["unchanged"]), len(result["historical"])))
        self.refuses("REFUSE_MISSING_OR_NONREGULAR", "bitmap")

    def test_path_escape_and_unknown_family_refuse(self):
        for relative in ("../outside", "/outside", "a/../b", "a//b", "a\\b", ".git/config", "a\tb"):
            with self.subTest(path=relative), self.assertRaisesRegex(ValueError, "REFUSE_PATH"):
                VERIFY.contained(self.root, relative)
        self.refuses("REFUSE_FAMILY", "unknown")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if sys.flags.optimize:
        parser.error("Python assertions are required")
    admitted = VERIFY.check(args.root)
    AdmissionContract.inputs = {**admitted["current"], **admitted["unchanged"], **admitted["historical"],
                               VERIFY.QUALIFICATION + "/SUCCESSOR.tsv":
                                   (args.root / VERIFY.QUALIFICATION / "SUCCESSOR.tsv").read_bytes()}
    AdmissionContract.output = args.output.absolute()
    AdmissionContract.output.mkdir(parents=True, exist_ok=False)
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(AdmissionContract))
    receipt = {"schema": "M3_ARRAY_MASK_SUCCESSOR_CONTRACT_V1", "status": "PASS" if result.wasSuccessful() else "FAIL",
               "tests": result.testsRun, "failures": len(result.failures), "errors": len(result.errors),
               "skipped": len(result.skipped), "product_sources_written": False,
               "successor_manifest_sha256": VERIFY.MANIFEST_SHA,
               "inputs_sha256": {p: VERIFY.sha(b) for p, b in AdmissionContract.inputs.items()}}
    (AdmissionContract.output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    if not result.wasSuccessful():
        raise SystemExit(1)


if __name__ == "__main__":
    main()
